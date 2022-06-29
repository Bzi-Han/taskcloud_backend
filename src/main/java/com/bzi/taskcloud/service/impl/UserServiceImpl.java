package com.bzi.taskcloud.service.impl;

import com.bzi.taskcloud.entity.User;
import com.bzi.taskcloud.mapper.UserMapper;
import com.bzi.taskcloud.service.IUserService;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
