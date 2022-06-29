package com.bzi.taskcloud.service.impl;

import com.bzi.taskcloud.entity.Config;
import com.bzi.taskcloud.mapper.ConfigMapper;
import com.bzi.taskcloud.service.IConfigService;
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
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements IConfigService {

}
