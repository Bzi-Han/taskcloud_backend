package com.bzi.taskcloud.service.impl;

import com.bzi.taskcloud.common.vo.PackageAvailableListVO;
import com.bzi.taskcloud.entity.Package;
import com.bzi.taskcloud.mapper.PackageMapper;
import com.bzi.taskcloud.service.IPackageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
@Service
public class PackageServiceImpl extends ServiceImpl<PackageMapper, Package> implements IPackageService {
    @Override
    public List<PackageAvailableListVO> getAvailableList(Long userId) {
        return baseMapper.getAvailableList(userId);
    }

    @Override
    public List<Package> getActivatedPackages() {
        return baseMapper.getActivatedPackages();
    }
}
