package com.bzi.taskcloud.mapper;

import com.bzi.taskcloud.entity.TaskLog;
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
public interface TaskLogMapper extends BaseMapper<TaskLog> {
    @Select("delete from task_log where state=0 or state=1")
    void deleteInvalidLogs();
}
