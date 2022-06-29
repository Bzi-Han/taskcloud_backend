package com.bzi.taskcloud.mapper;

import com.bzi.taskcloud.common.vo.PackageAvailableListVO;
import com.bzi.taskcloud.entity.Package;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
public interface PackageMapper extends BaseMapper<Package> {
    @Select("select id,name from package where user_id=#{userId}")
    List<PackageAvailableListVO> getAvailableList(Long userId);

    @Select("select id,user_id,name,tasks_config,activated,run_everyday,run_on_time from package where activated and (run_on_time > now() or run_everyday)")
    List<Package> getActivatedPackages();
}
