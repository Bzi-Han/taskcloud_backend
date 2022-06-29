package com.bzi.taskcloud.service.impl;

import com.bzi.taskcloud.entity.TaskLog;
import com.bzi.taskcloud.mapper.TaskLogMapper;
import com.bzi.taskcloud.service.ITaskLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
@Service
public class TaskLogServiceImpl extends ServiceImpl<TaskLogMapper, TaskLog> implements ITaskLogService {

    @Override
    public void deleteInvalidLogs() {
        baseMapper.deleteInvalidLogs();
    }

}
