package com.bzi.taskcloud.service.impl;

import com.bzi.taskcloud.entity.Help;
import com.bzi.taskcloud.mapper.HelpMapper;
import com.bzi.taskcloud.service.IHelpService;
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
public class HelpServiceImpl extends ServiceImpl<HelpMapper, Help> implements IHelpService {
    @Override
    public boolean updateAuthor(Long userId, String nickname) {
        baseMapper.updateAuthor(userId, nickname);
        return true;
    }
}
