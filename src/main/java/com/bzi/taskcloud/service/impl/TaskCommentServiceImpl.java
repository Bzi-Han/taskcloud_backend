package com.bzi.taskcloud.service.impl;

import com.bzi.taskcloud.entity.TaskComment;
import com.bzi.taskcloud.mapper.TaskCommentMapper;
import com.bzi.taskcloud.service.ITaskCommentService;
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
public class TaskCommentServiceImpl extends ServiceImpl<TaskCommentMapper, TaskComment> implements ITaskCommentService {
    @Override
    public boolean updateAuthor(Long userId, String nickname) {
        baseMapper.updateAuthor(userId, nickname);
        return true;
    }

    @Override
    public Float getAvgRating(Long taskId) {
        return baseMapper.getAvgRating(taskId);
    }
}
