package com.bzi.taskcloud.mapper;

import com.bzi.taskcloud.entity.Help;
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
public interface HelpMapper extends BaseMapper<Help> {
    @Select("update help set author=#{nickname} where author_id=#{userId}")
    void updateAuthor(Long userId, String nickname);
}
