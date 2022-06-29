package com.bzi.taskcloud.common.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.bzi.taskcloud.common.vo.UserDetailInfoVO;
import com.bzi.taskcloud.entity.User;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class AccountUtil {
    private static String PASSWORD_SALT;

    @Value("${data-security.password.salt}")
    public void setPasswordSalt(String salt) {
        if (StringUtils.isBlank(salt))
            throw new Error("请设置密码使用的盐");

        PASSWORD_SALT = salt;
    }

    public static String makePassword(String password) throws NoSuchAlgorithmException {
        MessageDigest hash = MessageDigest.getInstance("SHA-512");

        byte[] result = hash.digest((PASSWORD_SALT + password + PASSWORD_SALT).getBytes(StandardCharsets.UTF_8));

        return HexUtils.toHexString(result);
    }

    public static User getProfile() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static UserDetailInfoVO copyUserInfo(User user) {
        UserDetailInfoVO userDetailInfoVO = new UserDetailInfoVO();
        BeanUtils.copyProperties(user, userDetailInfoVO);

        return userDetailInfoVO;
    }
}
