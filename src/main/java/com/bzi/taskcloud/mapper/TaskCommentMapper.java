package com.bzi.taskcloud.mapper;

import com.bzi.taskcloud.entity.TaskComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
public interface TaskCommentMapper extends BaseMapper<TaskComment> {
    @Select("update task_comment set author=#{nickname} where author_id=#{userId}")
    void updateAuthor(Long userId, String nickname);

    @Select("select avg(rating) from task_comment where task_id=#{taskId}")
    Float getAvgRating(Long taskId);
}
