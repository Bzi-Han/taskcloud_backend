package com.bzi.taskcloud.service;

import com.bzi.taskcloud.entity.Help;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
public interface IHelpService extends IService<Help> {
    boolean updateAuthor(Long userId, String nickname);
}
