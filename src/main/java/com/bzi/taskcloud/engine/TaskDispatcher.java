package com.bzi.taskcloud.engine;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.bzi.taskcloud.common.lang.TaskState;
import com.bzi.taskcloud.common.lang.TaskType;
import com.bzi.taskcloud.common.utils.AccountUtil;
import com.bzi.taskcloud.common.utils.LoggerUtil;
import com.bzi.taskcloud.entity.Config;
import com.bzi.taskcloud.entity.Package;
import com.bzi.taskcloud.entity.Task;
import com.bzi.taskcloud.entity.TaskLog;
import com.bzi.taskcloud.security.data.PassportCrypto;
import com.bzi.taskcloud.service.IConfigService;
import com.bzi.taskcloud.service.ITaskLogService;
import com.bzi.taskcloud.service.ITaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.HashedWheelTimer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class TaskDispatcher {
    private final EngineTerminal engineTerminal;
    private final ITaskService taskService;
    private final ITaskLogService taskLogService;
    private final IConfigService configService;

    @Autowired
    public TaskDispatcher(EngineTerminal engineTerminal, ITaskService taskService, ITaskLogService taskLogService, IConfigService configService) {
        this.engineTerminal = engineTerminal;
        this.taskService = taskService;
        this.taskLogService = taskLogService;
        this.configService = configService;
    }

    private final HashedWheelTimer timingTaskPool = new HashedWheelTimer(1, TimeUnit.SECONDS, 60);

    public boolean postTask(Long interval, Task task, String passport, String interfaces, TaskLog taskLog) {
        final var userId = AccountUtil.getProfile().getId();

        timingTaskPool.newTimeout(timeout -> {
            var runnerId = engineTerminal.run(
                    userId,
                    task.getId(),
                    TaskType.values()[task.getType()],
                    task.getName(),
                    task.getScript(),
                    passport,
                    interfaces
            );

            // 检测任务是否投递成功
            if (0 != runnerId) {
                engineTerminal.onTaskComplete(runnerId, result -> {
                    taskLog.setState(result ? TaskState.succeed.ordinal() : TaskState.failed.ordinal());
                    taskLogService.updateById(taskLog);
                });

                taskLog.setState(TaskState.post.ordinal());
                taskLog.setExecuteOnTime(LocalDateTime.now());
            } else {
                taskLog.setState(TaskState.failed.ordinal());
                taskLog.setExecuteOnTime(LocalDateTime.now());
            }

            taskLogService.updateById(taskLog);
        }, interval, TimeUnit.SECONDS);

        return true;
    }

    public boolean postPackage(Package runPackage, String fromWhere) {
        return this.postPackage(runPackage, fromWhere, false);
    }

    public boolean postPackage(Package runPackage, String fromWhere, boolean runAnyWay) {
        // 计算任务包执行时间间隔
        long interval = 0L;
        var dateNow = LocalDateTime.now();

        if (!runAnyWay) {
            if (runPackage.getRunEveryday()) {
                interval = Duration.between(
                        dateNow.withYear(2112).withMonth(9).withDayOfMonth(3),
                        runPackage.getRunOnTime().withYear(2112).withMonth(9).withDayOfMonth(3)
                ).getSeconds();
            } else {
                // 对于非每天运行的任务我们需要确保开启定时任务的时候与定时的时间是同一天内，避免重复的添加该定时任务
                if (dateNow.getYear() == runPackage.getRunOnTime().getYear()
                    && dateNow.getMonth() == runPackage.getRunOnTime().getMonth()
                    && dateNow.getDayOfMonth() == runPackage.getRunOnTime().getDayOfMonth()
                ) {
                    interval = Duration.between(dateNow, runPackage.getRunOnTime()).getSeconds();
                } else
                    throw new TaskDispatcherException("该任务定时日期与当前日期不同。");
            }
            if (1 > interval)
                throw new TaskDispatcherException("该任务包已超过预定执行时间。");
        }

        // 解析需要执行的任务
        List<String> taskIds = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode tasksConfig;
        try {
            tasksConfig = objectMapper.readTree(runPackage.getTasksConfig());
            tasksConfig.fieldNames().forEachRemaining(fieldName -> {
                if (tasksConfig.get(fieldName).get("run").toString().contains("true"))
                    taskIds.add(fieldName);
            });
        } catch (JsonProcessingException e) {
            LoggerUtil.failed("任务分发失败", e);
            throw new TaskDispatcherException(e.getMessage());
        }
        if (taskIds.isEmpty())
            throw new TaskDispatcherException("该任务包没有任何需要运行的任务。");

        var runTasks = taskService.list(new QueryWrapper<Task>()
                .in("id", taskIds)
                .eq("state", TaskState.accept.ordinal())
        );
        if (runTasks.isEmpty())
            throw new TaskDispatcherException("该任务包没有任何可用的任务。");

        // 处理执行任务所需要的配置
        var passports = new ArrayList<String>();
        var interfaces = new ArrayList<String>();
        var taskLogs = new ArrayList<TaskLog>();

        for (Task task : runTasks) {
            // 配置日志对象
            var taskLog = new TaskLog();
            taskLog.setUserId(AccountUtil.getProfile().getId());
            taskLog.setPackageId(runPackage.getId());
            taskLog.setTaskId(task.getId());
            taskLog.setPackageName(runPackage.getName());
            taskLog.setTaskName(task.getName());
            taskLog.setTaskVersion(task.getVersion());
            taskLog.setState(TaskState.wait.ordinal());
            taskLog.setExecuteOnTime(LocalDateTime.now());
            taskLog.setFromWhere(fromWhere);

            // 获取通行证配置
            var passportConfig = configService.getOne(new QueryWrapper<Config>()
                    .eq("user_id", AccountUtil.getProfile().getId())
                    .eq("domain", task.getDomain())
            );
            if (StringUtils.isBlank(passportConfig.getPassport()))
                throw new TaskDispatcherException("主域名:" + task.getDomain() + " 尚未设置通行证。");
            try {
                passports.add(PassportCrypto.decrypt(passportConfig.getPassport()));
            } catch (Exception e) {
                LoggerUtil.failed("任务通行证解密失败", e);
                throw new TaskDispatcherException("主域名:" + task.getDomain() + " 通行证加载失败。");
            }

            // 解析需要执行的接口
            var runInterfaceBuilder = new StringBuilder();
            var logInterfaceBuilder = new StringBuilder();
            
            Map<String, String> methods = null;
            try {
                methods = TaskAnalyzer.scanInterface(task);
            } catch (JsonProcessingException e) {
                LoggerUtil.failed("任务分发失败", e);
                throw new TaskDispatcherException(e.getMessage());
            }
            
            var runMethodArray = tasksConfig.get(task.getId().toString()).get("run");
            for (int i = 0; i < runMethodArray.size(); i++) {
                if (runMethodArray.get(i).has("true")) {
                    var method = runMethodArray.get(i).get("true").asText();

                    methods.forEach((key, value) -> {
                        if (value.equals(method)) {
                            runInterfaceBuilder.append(key).append(",");
                            logInterfaceBuilder.append(value).append(",");
                        }
                    });
                }
            }

            // 添加配置
            if (0 < runInterfaceBuilder.length())
                runInterfaceBuilder.setLength(runInterfaceBuilder.length() - 1);
            if (0 < logInterfaceBuilder.length())
                logInterfaceBuilder.setLength(logInterfaceBuilder.length() - 1);

            interfaces.add(runInterfaceBuilder.toString());
            taskLog.setFunctions(logInterfaceBuilder.toString());
            taskLogs.add(taskLog);
        }

        // 投递任务
        taskLogService.saveBatch(taskLogs); // 保存日志到数据库中
        for (int i = 0; i < runTasks.size(); i++) {
            this.postTask(interval, runTasks.get(i), passports.get(i), interfaces.get(i), taskLogs.get(i));
        }

        return true;
    }
}
