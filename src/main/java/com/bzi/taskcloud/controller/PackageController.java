package com.bzi.taskcloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzi.taskcloud.common.dto.PackageAddDTO;
import com.bzi.taskcloud.common.dto.PackageAppendTaskDTO;
import com.bzi.taskcloud.common.dto.PackageUpdateDTO;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.lang.TaskState;
import com.bzi.taskcloud.common.utils.AccountUtil;
import com.bzi.taskcloud.common.utils.PageUtil;
import com.bzi.taskcloud.common.vo.PackageDetailInfoVO;
import com.bzi.taskcloud.engine.TaskDispatcher;
import com.bzi.taskcloud.entity.Config;
import com.bzi.taskcloud.entity.Package;
import com.bzi.taskcloud.entity.Task;
import com.bzi.taskcloud.security.data.DecryptRequest;
import com.bzi.taskcloud.security.data.EncryptResponse;
import com.bzi.taskcloud.service.IConfigService;
import com.bzi.taskcloud.service.IPackageService;
import com.bzi.taskcloud.service.ITaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
@RestController
@DecryptRequest
@EncryptResponse
@RequestMapping("/package")
@Api(value = "任务包模块", description = "任务包模块")
public class PackageController {
    private final IPackageService packageService;
    private final ITaskService taskService;
    private final IConfigService configService;
    private final DataSourceTransactionManager dataSourceTransactionManager;
    private final TransactionDefinition transactionDefinition;
    private final TaskDispatcher taskDispatcher;

    @Autowired
    public PackageController(IPackageService packageService, ITaskService taskService, IConfigService configService, DataSourceTransactionManager dataSourceTransactionManager, TransactionDefinition transactionDefinition, TaskDispatcher taskDispatcher) {
        this.packageService = packageService;
        this.taskService = taskService;
        this.configService = configService;
        this.dataSourceTransactionManager = dataSourceTransactionManager;
        this.transactionDefinition = transactionDefinition;
        this.taskDispatcher = taskDispatcher;
    }

    @ApiOperation(value = "添加一个任务包", notes = "用户接口")
    @PostMapping("/add")
    public Result add(@Validated @RequestBody PackageAddDTO packageAddDTO) {
        Package taskPackage = new Package();

        // 设置任务包属性
        BeanUtils.copyProperties(packageAddDTO, taskPackage);
        taskPackage.setUserId(AccountUtil.getProfile().getId());
        taskPackage.setTasksConfig("{}");

        // 添加任务包到数据库中
        Assert.isTrue(packageService.save(taskPackage),  "添加任务包失败！");

        // 返回数据
        PackageDetailInfoVO packageDetailInfoVO = new PackageDetailInfoVO();
        BeanUtils.copyProperties(taskPackage, packageDetailInfoVO);

        return Result.succeed(packageDetailInfoVO);
    }

    @ApiOperation(value = "添加一个任务到指定任务包中", notes = "用户接口")
    @PostMapping("/append")
    public Result appendTask(@Validated @RequestBody PackageAppendTaskDTO packageAppendTaskDTO) throws JsonProcessingException {
        Package taskPackage = packageService.getOne(new QueryWrapper<Package>()
                .eq("id", packageAppendTaskDTO.getPackageId())
                .eq("user_id", AccountUtil.getProfile().getId())
        );
        Assert.notNull(taskPackage, "任务包不存在！");

        Task task = taskService.getOne(new QueryWrapper<Task>()
                .eq("id", packageAppendTaskDTO.getTaskId())
                .eq("state", TaskState.accept.ordinal())
        );
        Assert.notNull(task, "任务不存在！");

        // 加载任务设置JSON对象
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode taskConfig = (ObjectNode) objectMapper.readTree(taskPackage.getTasksConfig());

        // 检查该任务是否已存在任务包中
        Assert.isTrue(!taskConfig.has(task.getId().toString()), "该任务已存在于任务包中！");

        // 获取任务通行证配置
        Config config = configService.getOne(new QueryWrapper<Config>()
                .eq("user_id", AccountUtil.getProfile().getId())
                .eq("domain", task.getDomain())
        );
        if (Objects.isNull(config)) {
            config = new Config();
            config.setUserId(AccountUtil.getProfile().getId());
            config.setDomain(task.getDomain());
            config.setPassport("");
            config.setReferenceCount(0);
        }

        // 构造任务配置
        String[] interfaces = task.getInterfaces().split(",");

        ArrayNode runNode = objectMapper.createArrayNode(); // 任务运行的接口
        for (String interfaceName : interfaces) {
            ObjectNode interfaceNode = objectMapper.createObjectNode(); // 每个接口的开关

            interfaceNode.put("false", interfaceName);

            runNode.add(interfaceNode);
        }

        ObjectNode taskNode = objectMapper.createObjectNode(); // 任务的信息
        taskNode.put("name", task.getName());
        taskNode.put("domain", task.getDomain());
        taskNode.set("run", runNode);

        taskConfig.set(task.getId().toString(), taskNode); // 添加到任务配置中

        // 更新任务包属性
        taskPackage.setTasksConfig(objectMapper.writeValueAsString(taskConfig));

        // 更新数据库
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try {
            // 更新通信证配置引用
            config.setReferenceCount(config.getReferenceCount() + 1);
            Assert.isTrue(configService.saveOrUpdate(config), "通信证配置更新失败！");

            // 更新任务包
            Assert.isTrue(packageService.updateById(taskPackage), "任务包更新失败！");

            dataSourceTransactionManager.commit(transactionStatus);
        } catch (Exception exception) {
            dataSourceTransactionManager.rollback(transactionStatus);
            throw exception;
        }

        return Result.succeed();
    }

    @ApiOperation(value = "更新任务包信息", notes = "用户接口")
    @PutMapping("/update")
    public Result update(@Validated @RequestBody PackageUpdateDTO packageUpdateDTO) throws JsonProcessingException {
        Package taskPackage = packageService.getOne(new QueryWrapper<Package>()
                .eq("id", packageUpdateDTO.getId())
                .eq("user_id", AccountUtil.getProfile().getId())
        );
        Assert.notNull(taskPackage, "任务包不存在！");

        // 检查是否更改了任务配置
        List<String> removeDomains = new ArrayList<>();
        if (taskPackage.getTasksConfig().equals(packageUpdateDTO.getTasksConfig())) {
            // 检测包内任务的变动并获得是否需要更新通行证配置引用的信息
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode original = objectMapper.readTree(taskPackage.getTasksConfig());
            JsonNode changed = objectMapper.readTree(packageUpdateDTO.getTasksConfig());

            original.fieldNames().forEachRemaining(fieldName -> {
                if (!changed.has(fieldName))
                    removeDomains.add(original.get(fieldName).get("domain").asText());
            });
        }

        // 更新任务包属性值
        BeanUtils.copyProperties(packageUpdateDTO, taskPackage, "id");

        // 更新数据库
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try {
            // 检查是否需要更新通行证配置引用
            if (!removeDomains.isEmpty()) {
                // 获取通信证配置
                List<Config> queryConfigs = configService.list(new QueryWrapper<Config>()
                        .eq("user_id", AccountUtil.getProfile().getId())
                        .in("domain", removeDomains)
                );
                List<Config> updateConfigs = new ArrayList<>();
                List<Config> removeConfigs = new ArrayList<>();

                queryConfigs.forEach(config -> {
                    if (1 >= config.getReferenceCount())
                        removeConfigs.add(config);
                    else {
                        config.setReferenceCount(config.getReferenceCount() - 1);
                        updateConfigs.add(config);
                    }
                });

                // 更新通信证配置引用
                if (!removeConfigs.isEmpty())
                    Assert.isTrue(configService.removeBatchByIds(removeConfigs), "通行证配置更新失败001！");
                Assert.isTrue(configService.updateBatchById(updateConfigs), "通信证配置更新失败002！");
            }

            // 更新任务包
            Assert.isTrue(packageService.updateById(taskPackage), "任务包更新失败！");

            dataSourceTransactionManager.commit(transactionStatus);
        } catch (Exception exception) {
            dataSourceTransactionManager.rollback(transactionStatus);
            throw exception;
        }

        return Result.succeed();
    }

    @ApiOperation(value = "分页获取任务包信息", notes = "用户接口")
    @GetMapping("/items/{pageIndex}")
    public Result items(@PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        IPage<Package> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        // 从数据库中查询数据
        IPage<Package> dataPage = packageService.page(queryPage, new QueryWrapper<Package>()
                .eq("user_id", AccountUtil.getProfile().getId())
        );

        return Result.succeed(PageUtil.filterResult(dataPage, PackageDetailInfoVO.class));
    }

    @ApiOperation(value = "获取所有可用的任务包信息", notes = "用户接口")
    @GetMapping("/available")
    public Result available() {
        return Result.succeed(
                packageService.getAvailableList(AccountUtil.getProfile().getId())
        );
    }

    @ApiOperation(value = "删除任务包", notes = "用户接口")
    @DeleteMapping("/delete/{packageId}")
    public Result delete(@PathVariable Long packageId) {
        Package taskPackage = packageService.getOne(new QueryWrapper<Package>()
                .eq("id", packageId)
                .eq("user_id", AccountUtil.getProfile().getId())
        );
        Assert.notNull(taskPackage, "任务包不存在！");

        // 更新数据库
        Assert.isTrue(packageService.removeById(taskPackage.getId()), "删除任务包失败！");

        return Result.succeed();
    }

    @ApiOperation(value = "执行任务包", notes = "用户接口")
    @PostMapping("/post/{packageId}")
    public Result postPackageToRun(@PathVariable Long packageId) {
        Package taskPackage = packageService.getOne(new QueryWrapper<Package>()
                .eq("id", packageId)
                .eq("user_id", AccountUtil.getProfile().getId())
        );
        Assert.notNull(taskPackage, "任务包不存在！");

        if (taskDispatcher.postPackage(taskPackage, "手动投递", true))
            return Result.succeed();

        return Result.failed("执行任务包失败！");
    }

}
