package com.bzi.taskcloud.service.impl;

import com.bzi.taskcloud.entity.Task;
import com.bzi.taskcloud.mapper.TaskMapper;
import com.bzi.taskcloud.service.ITaskService;
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
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {
    @Override
    public boolean updateAuthor(Long userId, String nickname) {
        baseMapper.updateAuthor(userId, nickname);
        return true;
    }
}
