package com.bzi.taskcloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzi.taskcloud.common.dto.ConfigUpdateDTO;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.utils.AccountUtil;
import com.bzi.taskcloud.common.utils.PageUtil;
import com.bzi.taskcloud.common.vo.ConfigDetailInfoVO;
import com.bzi.taskcloud.entity.Config;
import com.bzi.taskcloud.security.data.DecryptRequest;
import com.bzi.taskcloud.security.data.EncryptResponse;
import com.bzi.taskcloud.security.data.PassportCrypto;
import com.bzi.taskcloud.service.IConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.DestroyFailedException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
@RestController
@DecryptRequest
@EncryptResponse
@RequestMapping("/config")
@Api(value = "通行证配置模块", description = "通行证配置模块")
public class ConfigController {
    @Autowired
    private IConfigService configService;

    @ApiOperation(value = "通信证配置信息更新", notes = "用户接口")
    @PutMapping("/update")
    public Result update(@Validated @RequestBody ConfigUpdateDTO configUpdateDTO) throws DestroyFailedException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Config config = configService.getOne(new QueryWrapper<Config>()
                .eq("id", configUpdateDTO.getId())
                .eq("user_id", AccountUtil.getProfile().getId())
        );
        Assert.notNull(config, "通行证配置不存在！");

        // 设置通行证
        config.setPassport(PassportCrypto.encrypt(configUpdateDTO.getPassport()));

        // 更新数据库
        Assert.isTrue(configService.updateById(config), "更新通行证信息失败！");

        return Result.succeed();
    }

    @ApiOperation(value = "分页查询通行证配置", notes = "用户接口")
    @GetMapping("/items/{pageIndex}")
    public Result items(@PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, DestroyFailedException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, NoSuchProviderException {
        IPage<Config> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        // 从数据库中查询数据
        IPage<Config> dataPage = configService.page(queryPage, new QueryWrapper<Config>()
                .eq("user_id", AccountUtil.getProfile().getId())
        );

        var result = PageUtil.filterResult(dataPage, ConfigDetailInfoVO.class);
        for (var config : result.getRecords()) {
            config.setPassport(PassportCrypto.decrypt(config.getPassport()));
        }

        return Result.succeed(result);
    }
}
