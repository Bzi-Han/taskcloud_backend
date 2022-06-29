package com.bzi.taskcloud.service;

import com.bzi.taskcloud.common.vo.PackageAvailableListVO;
import com.bzi.taskcloud.entity.Package;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
public interface IPackageService extends IService<Package> {
    List<PackageAvailableListVO> getAvailableList(Long userId);

    List<Package> getActivatedPackages();
}
