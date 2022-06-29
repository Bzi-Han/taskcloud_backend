package com.bzi.taskcloud.engine;

import com.bzi.taskcloud.common.utils.LoggerUtil;
import com.bzi.taskcloud.entity.Package;
import com.bzi.taskcloud.service.IPackageService;
import com.bzi.taskcloud.service.ITaskLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class Atlas {
    public static boolean active = true;
    private static boolean firstStart = true;
    private LocalDateTime lastStart;

    @Autowired
    private ITaskLogService taskLogService;
    @Autowired
    private IPackageService packageService;
    @Autowired
    private TaskDispatcher taskDispatcher;

    Atlas() {
        new Thread(() -> {
            while (active) {
                try {
                    var dateNow = LocalDateTime.now();

                    if (firstStart) {
                        taskLogService.deleteInvalidLogs(); // 删除没有用的日志
                        var activatedPackages = packageService.getActivatedPackages();

                        for (var runPackage : activatedPackages) {
                            if (runPackage.getRunEveryday()) {
                                if (isTodayRemainTask(runPackage.getRunOnTime()))
                                    postRun(runPackage);
                            } else
                                postRun(runPackage);
                        }

                        lastStart = dateNow;
                        firstStart = false;
                    } else if (0 == dateNow.getHour() && lastStart.getDayOfYear() != dateNow.getDayOfYear()) {
                        var activatedPackages = packageService.getActivatedPackages();

                        for (var runPackage : activatedPackages)
                            postRun(runPackage);

                        lastStart = dateNow;
                    }
                } catch (Exception e) {
                    LoggerUtil.failed("Atlas", e);
                }

                try {
                    Thread.sleep(60000);
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private boolean isTodayRemainTask(LocalDateTime target) {
        return 30 < Duration.between(
                target.withYear(2112).withMonth(9).withDayOfMonth(3),
                LocalDateTime.now().withYear(2112).withMonth(9).withDayOfMonth(3)
        ).getSeconds();
    }

    private void postRun(Package runPackage) {
        try {
            taskDispatcher.postPackage(runPackage, "系统投递");
        } catch (Exception e) {
            LoggerUtil.failed("Atlas post run", e);
        }
    }
}
