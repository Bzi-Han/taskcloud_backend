package com.bzi.taskcloud.service;

import com.bzi.taskcloud.entity.TaskLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
public interface ITaskLogService extends IService<TaskLog> {
    void deleteInvalidLogs();
}
