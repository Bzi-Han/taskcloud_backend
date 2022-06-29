package com.bzi.taskcloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzi.taskcloud.common.dto.TaskPublishDTO;
import com.bzi.taskcloud.common.dto.TaskReviewDTO;
import com.bzi.taskcloud.common.dto.TaskUpdateDTO;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.lang.TaskState;
import com.bzi.taskcloud.common.lang.UserType;
import com.bzi.taskcloud.common.utils.AccountUtil;
import com.bzi.taskcloud.common.utils.PageUtil;
import com.bzi.taskcloud.common.vo.TaskDetailInfoDeveloperVO;
import com.bzi.taskcloud.common.vo.TaskDetailInfoUserVO;
import com.bzi.taskcloud.engine.TaskAnalyzer;
import com.bzi.taskcloud.entity.Task;
import com.bzi.taskcloud.security.data.DecryptRequest;
import com.bzi.taskcloud.security.data.EncryptResponse;
import com.bzi.taskcloud.service.ITaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import java.lang.reflect.InvocationTargetException;
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
@RequestMapping("/task")
@Api(value = "任务模块", description = "任务模块")
public class TaskController {
    @Autowired
    private ITaskService taskService;

    @ApiOperation(value = "任务发布", notes = "开发者接口")
    @PostMapping("/publish")
    @RolesAllowed("developer")
    public Result publish(@Validated @RequestBody TaskPublishDTO taskPublishDTO) {
        Task task = new Task();

        // 设置任务信息
        BeanUtils.copyProperties(taskPublishDTO, task);
        if (StringUtils.isBlank(task.getWarning()))
            task.setWarning("");
        if (StringUtils.isBlank(task.getVersion()))
            task.setVersion("1.0.0");
        task.setDomain("");
        task.setInterfaces("");
        task.setAuthor(AccountUtil.getProfile().getNickname());
        task.setAuthorId(AccountUtil.getProfile().getId());
        task.setState(TaskState.review.ordinal());
        task.setStateMessage("任务审核中");

        // 添加到数据库
        Assert.isTrue(
                taskService.save(task),
                "任务发布失败，请联系管理员！"
        );

        // 返回数据
        TaskDetailInfoDeveloperVO taskDetailInfoDeveloperVO = new TaskDetailInfoDeveloperVO();
        BeanUtils.copyProperties(task, taskDetailInfoDeveloperVO);

        return Result.succeed(taskDetailInfoDeveloperVO);
    }

    @ApiOperation(value = "任务审核", notes = "管理员接口")
    @PutMapping("/review")
    @RolesAllowed("admin")
    public Result review(@Validated @RequestBody TaskReviewDTO taskReviewDTO) throws JsonProcessingException {
        Task task = taskService.getById(taskReviewDTO.getId());
        Assert.notNull(task, "任务不存在，请检查参数是否正确！");

        // 更新值
        task.setState(taskReviewDTO.getState());
        if (StringUtils.isBlank(taskReviewDTO.getStateMessage()) ||
                "任务审核中".equals(taskReviewDTO.getStateMessage()) ||
                "任务正在重新审核中".equals(taskReviewDTO.getStateMessage())) {
            task.setStateMessage(TaskState.accept.ordinal() == task.getState() ? "审核已通过" : "审核未通过");
        } else
            task.setStateMessage(taskReviewDTO.getStateMessage());

        // 扫描通行证主域名与接口
        task.setDomain(TaskAnalyzer.scanDomain(task));
        task.setInterfaces("");
        TaskAnalyzer.scanInterface(task).forEach((key, value) -> {
            if (task.getInterfaces().isEmpty())
                task.setInterfaces(value);
            else
                task.setInterfaces(task.getInterfaces() + "," + value);
        });

        // 更新数据库
        Assert.isTrue(
                taskService.updateById(task),
                "更新任务信息失败，请查看服务器日志！"
        );

        return Result.succeed(task);
    }

    @ApiOperation(value = "任务信息更新", notes = "开发者接口")
    @PutMapping("/update")
    @RolesAllowed("developer")
    public Result update(@Validated @RequestBody TaskUpdateDTO taskUpdateDTO) {
        Task task = taskService.getById(taskUpdateDTO.getId());
        Assert.notNull(task, "任务不存在！");

        // 检查要更新的任务所有者是否合法
        Assert.isTrue(Objects.equals(task.getAuthorId(), AccountUtil.getProfile().getId()), "任务不存在！");

        // 更新任务信息
        BeanUtils.copyProperties(taskUpdateDTO, task, "id");
        if (StringUtils.isBlank(task.getVersion()))
            task.setVersion("1.0.0");
        task.setState(TaskState.review.ordinal());
        task.setStateMessage("任务正在重新审核中");

        // 更新数据库
        Assert.isTrue(
                taskService.updateById(task),
                "任务信息更新失败！"
        );

        // 返回数据
        TaskDetailInfoDeveloperVO taskDetailInfoDeveloperVO = new TaskDetailInfoDeveloperVO();
        BeanUtils.copyProperties(task, taskDetailInfoDeveloperVO);

        return Result.succeed(taskDetailInfoDeveloperVO);
    }

    @ApiOperation(value = "分页获取任务信息", notes = "用户接口")
    @GetMapping("/items/{pageIndex}")
    public Result items(@PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // 需要根据用户类型不同返回不同的VO，限定一页只能查10条记录
        IPage<Task> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        if (UserType.admin.ordinal() == AccountUtil.getProfile().getType()) {
            // 管理员可见数据
            IPage<Task> dataPage = taskService.page(queryPage, new QueryWrapper<Task>()
                    // .ne("state", TaskState.delete.ordinal())
                    .eq("state", TaskState.review.ordinal())
            );

            return Result.succeed(PageUtil.filterResult(dataPage));
        }

        // 用户与开发者可见数据
        IPage<Task> dataPage = taskService.page(queryPage, new QueryWrapper<Task>()
                .eq("state", TaskState.accept.ordinal())
        );

        return Result.succeed(PageUtil.filterResult(dataPage, TaskDetailInfoUserVO.class));
    }

    @ApiOperation(value = "分页获取自己发布的任务信息", notes = "开发者接口")
    @GetMapping("/devitems/{pageIndex}")
    @RolesAllowed("developer")
    public Result devItems(@PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        IPage<Task> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        // 从数据库中查询数据
        IPage<Task> dataPage = taskService.page(queryPage, new QueryWrapper<Task>()
                .eq("author_id", AccountUtil.getProfile().getId())
                .ne("state", TaskState.delete.ordinal())
        );

        return Result.succeed(PageUtil.filterResult(dataPage, TaskDetailInfoDeveloperVO.class));
    }

    @ApiOperation(value = "任务详细信息", notes = "用户接口")
    @GetMapping("/detail/{taskId}")
    public Result detail(@PathVariable Long taskId) {
        if (UserType.admin.ordinal() == AccountUtil.getProfile().getType()) {
            // 管理员可见数据
            Task task = taskService.getOne(new QueryWrapper<Task>()
                    .eq("id", taskId)
                    .ne("state", TaskState.delete.ordinal())
            );
            Assert.notNull(task, "任务不存在！");

            return Result.succeed(task);
        }

        // 用户于开发者可见数据
        Task task = taskService.getOne(new QueryWrapper<Task>()
                .eq("id", taskId)
                .eq("state", TaskState.accept.ordinal())
        );
        Assert.notNull(task, "任务不存在！");

        TaskDetailInfoUserVO taskDetailInfoUserVO = new TaskDetailInfoUserVO();
        BeanUtils.copyProperties(task, taskDetailInfoUserVO);

        return Result.succeed(taskDetailInfoUserVO);
    }

    @ApiOperation(value = "自己发布的任务的详细信息", notes = "开发者接口")
    @GetMapping("/devdetail/{taskId}")
    @RolesAllowed("developer")
    public Result devDetail(@PathVariable Long taskId) {
        Task task = taskService.getOne(new QueryWrapper<Task>()
                .eq("id", taskId)
                .eq("author_id", AccountUtil.getProfile().getId())
                .ne("state", TaskState.delete.ordinal())
        );
        Assert.notNull(task, "任务不存在！");

        TaskDetailInfoDeveloperVO taskDetailInfoDeveloperVO = new TaskDetailInfoDeveloperVO();
        BeanUtils.copyProperties(task, taskDetailInfoDeveloperVO);

        return Result.succeed(taskDetailInfoDeveloperVO);
    }

    @ApiOperation(value = "分页搜索任务信息", notes = "用户接口")
    @GetMapping("/search/{keywords}/{pageIndex}")
    public Result search(@PathVariable String keywords, @PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // 需要根据用户类型不同返回不同的VO，限定一页只能查10条记录
        IPage<Task> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        if (UserType.admin.ordinal() == AccountUtil.getProfile().getType()) {
            // 管理员可见数据
            IPage<Task> dataPage = taskService.page(queryPage, new QueryWrapper<Task>()
                    .like("name", keywords)
                    .or()
                    .like("author", keywords)
                    // .ne("state", TaskState.delete.ordinal())
                    .eq("state", TaskState.review.ordinal())
            );

            return Result.succeed(PageUtil.filterResult(dataPage));
        }

        // 用户与开发者可见数据
        IPage<Task> dataPage = taskService.page(queryPage, new QueryWrapper<Task>()
                .like("name", keywords)
                .or()
                .like("author", keywords)
                .eq("state", TaskState.accept.ordinal())
        );

        return Result.succeed(PageUtil.filterResult(dataPage, TaskDetailInfoUserVO.class));
    }

    @ApiOperation(value = "分页搜索自己发布的任务信息", notes = "开发者接口")
    @GetMapping("/devsearch/{keywords}/{pageIndex}")
    @RolesAllowed("developer")
    public Result devSearch(@PathVariable String keywords, @PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        IPage<Task> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        // 从数据库中查询数据
        IPage<Task> dataPage = taskService.page(queryPage, new QueryWrapper<Task>()
                .like("name", keywords)
                .or()
                .like("author", keywords)
                .eq("author_id", AccountUtil.getProfile().getId())
                .ne("state", TaskState.delete.ordinal())
        );

        return Result.succeed(PageUtil.filterResult(dataPage, TaskDetailInfoDeveloperVO.class));
    }

    @ApiOperation(value = "任务删除", notes = "开发者与管理员接口")
    @DeleteMapping("/delete/{taskId}")
    @RolesAllowed({"developer", "admin"})
    public Result delete(@PathVariable Long taskId) {
        Task task = taskService.getById(taskId);
        Assert.notNull(task, "任务不存在！");

        // 更新任务信息
        task.setState(TaskState.delete.ordinal());

        // 如果不是管理员，则需要校验是否为帮助文章的所有者
        if (UserType.admin.ordinal() != AccountUtil.getProfile().getType()) {
            // 检查是否为要删除的任务的所有者
            Assert.isTrue(Objects.equals(task.getAuthorId(), AccountUtil.getProfile().getId()), "任务不存在！");
        }

        // 更新数据库
        Assert.isTrue(taskService.updateById(task), "任务删除失败！");

        return Result.succeed();
    }
}
