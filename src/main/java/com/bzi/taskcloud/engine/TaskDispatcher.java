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
    @Autowired
    private EngineTerminal engineTerminal;
    @Autowired
    private ITaskService taskService;
    @Autowired
    private ITaskLogService taskLogService;
    @Autowired
    private IConfigService configService;

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

            // ??????????????????????????????
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
        // ?????????????????????????????????
        long interval = 0L;
        var dateNow = LocalDateTime.now();

        if (!runAnyWay) {
            if (runPackage.getRunEveryday()) {
                interval = Duration.between(
                        dateNow.withYear(2112).withMonth(9).withDayOfMonth(3),
                        runPackage.getRunOnTime().withYear(2112).withMonth(9).withDayOfMonth(3)
                ).getSeconds();
            } else {
                // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                if (dateNow.getYear() == runPackage.getRunOnTime().getYear()
                    && dateNow.getMonth() == runPackage.getRunOnTime().getMonth()
                    && dateNow.getDayOfMonth() == runPackage.getRunOnTime().getDayOfMonth()
                ) {
                    interval = Duration.between(dateNow, runPackage.getRunOnTime()).getSeconds();
                } else
                    throw new TaskDispatcherException("?????????????????????????????????????????????");
            }
            if (1 > interval)
                throw new TaskDispatcherException("??????????????????????????????????????????");
        }

        // ???????????????????????????
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
            LoggerUtil.failed("??????????????????", e);
            throw new TaskDispatcherException(e.getMessage());
        }
        if (taskIds.isEmpty())
            throw new TaskDispatcherException("????????????????????????????????????????????????");

        var runTasks = taskService.list(new QueryWrapper<Task>()
                .in("id", taskIds)
                .eq("state", TaskState.accept.ordinal())
        );
        if (runTasks.isEmpty())
            throw new TaskDispatcherException("??????????????????????????????????????????");

        // ????????????????????????????????????
        var passports = new ArrayList<String>();
        var interfaces = new ArrayList<String>();
        var taskLogs = new ArrayList<TaskLog>();

        for (Task task : runTasks) {
            // ??????????????????
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

            // ?????????????????????
            var passportConfig = configService.getOne(new QueryWrapper<Config>()
                    .eq("user_id", AccountUtil.getProfile().getId())
                    .eq("domain", task.getDomain())
            );
            if (StringUtils.isBlank(passportConfig.getPassport()))
                throw new TaskDispatcherException("?????????:" + task.getDomain() + " ????????????????????????");
            try {
                passports.add(PassportCrypto.decrypt(passportConfig.getPassport()));
            } catch (Exception e) {
                LoggerUtil.failed("???????????????????????????", e);
                throw new TaskDispatcherException("?????????:" + task.getDomain() + " ????????????????????????");
            }

            // ???????????????????????????
            var runInterfaceBuilder = new StringBuilder();
            var logInterfaceBuilder = new StringBuilder();
            
            Map<String, String> methods = null;
            try {
                methods = TaskAnalyzer.scanInterface(task);
            } catch (JsonProcessingException e) {
                LoggerUtil.failed("??????????????????", e);
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

            // ????????????
            if (0 < runInterfaceBuilder.length())
                runInterfaceBuilder.setLength(runInterfaceBuilder.length() - 1);
            if (0 < logInterfaceBuilder.length())
                logInterfaceBuilder.setLength(logInterfaceBuilder.length() - 1);

            interfaces.add(runInterfaceBuilder.toString());
            taskLog.setFunctions(logInterfaceBuilder.toString());
            taskLogs.add(taskLog);
        }

        // ????????????
        taskLogService.saveBatch(taskLogs); // ???????????????????????????
        for (int i = 0; i < runTasks.size(); i++) {
            this.postTask(interval, runTasks.get(i), passports.get(i), interfaces.get(i), taskLogs.get(i));
        }

        return true;
    }
}
