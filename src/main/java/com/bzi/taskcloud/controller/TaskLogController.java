package com.bzi.taskcloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.utils.AccountUtil;
import com.bzi.taskcloud.common.vo.TaskLogVO;
import com.bzi.taskcloud.entity.TaskLog;
import com.bzi.taskcloud.security.data.DecryptRequest;
import com.bzi.taskcloud.security.data.EncryptResponse;
import com.bzi.taskcloud.service.ITaskLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

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
@RequestMapping("/taskLog")
@Api(value = "任务投递日志模块", description = "任务投递日志模块")
public class TaskLogController {
    @Value("${engine.logRelativePath}")
    private String logRelativePath;

    @Autowired
    private ITaskLogService taskLogService;

    @ApiOperation(value = "获取指定天数的任务投递日志", notes = "用户接口")
    @GetMapping("/items/{days}")
    public Result items(@PathVariable Integer days) {
        Assert.isTrue(0 < days && 31 >= days, "参数错误。"); // 要取的天数必须在1-31之间

        // 计算最后取出的时间
        LocalDateTime now = LocalDateTime.now();
        now = now.withDayOfYear(now.getDayOfYear() - days);

        // 查询
        List<TaskLog> data = taskLogService.list(new QueryWrapper<TaskLog>()
                .eq("user_id", AccountUtil.getProfile().getId())
                .ge("execute_on_time", now)
        );

        List<TaskLogVO> filterData = new ArrayList<>(data.size());
        for (TaskLog item : data) {
            TaskLogVO taskLogVO = new TaskLogVO();

            BeanUtils.copyProperties(item, taskLogVO);

            filterData.add(taskLogVO);
        }

        return Result.succeed(filterData);
    }

    @ApiOperation(value = "获取用户执行的任务输出日志", notes = "用户接口")
    @GetMapping("/output/{simple}")
    public Result output(@PathVariable Integer simple) throws IOException {
        File logFile = new File(System.getProperty("user.dir") + logRelativePath + AccountUtil.getProfile().getId() + "/output.log");
        Assert.isTrue(logFile.exists(), "日志文件不存在。");

        // 打开日志文件
        RandomAccessFile reader = new RandomAccessFile(logFile, "r");

        // 从本地文件目录中读取任务的输出日志
        if (1 == simple) {
            // 只读取最大2K的日志
            byte[] buffer = new byte[2048];
            reader.seek(1 > reader.length() - 2048 ? 0 : reader.length() - 2048);
            int length = reader.read(buffer);
            reader.close();

            return Result.succeed((Object)new String(buffer, 0, length));
        } else {
            // 读取最大32K的日志
            byte[] buffer = new byte[32768];
            reader.seek(1 > reader.length() - 32768 ? 0 : reader.length() - 32768);
            int length = reader.read(buffer);
            reader.close();

            return Result.succeed((Object)new String(buffer, 0, length));
        }
    }
}
