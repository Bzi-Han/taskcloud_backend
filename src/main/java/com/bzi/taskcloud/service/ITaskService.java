package com.bzi.taskcloud.service;

import com.bzi.taskcloud.entity.Task;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
public interface ITaskService extends IService<Task> {
    boolean updateAuthor(Long userId, String nickname);
}
