package com.bzi.taskcloud.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.bzi.taskcloud.entity.User;
import com.bzi.taskcloud.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class JwtUtil {
    private static String SIGN_KEY;
    private static long EXPIRY_DATE;

    private static IUserService USER_SERVICE;

    @Value("${data-security.jwt.signKey}")
    public void setSignKey(String signKey) {
        if (StringUtils.isBlank(signKey))
            throw new Error("请设置JwtToken使用的签名密钥");

        SIGN_KEY = signKey;
    }

    @Value("${data-security.jwt.expiryDate}")
    public void setSignKey(long expiryDate) {
        EXPIRY_DATE = expiryDate;
    }

    @Autowired
    public void setUserService(IUserService userService) {
        USER_SERVICE = userService;
    }

    public static String generateToken(User user) {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.systemDefault());
        ZonedDateTime expired = now.plusDays(EXPIRY_DATE);

        return JWT.create()
                .withSubject(user.getId().toString())
                .withIssuedAt(Date.from(now.toInstant()))
                .withExpiresAt(Date.from(expired.toInstant()))
                .sign(Algorithm.HMAC512(SIGN_KEY));
    }

    public static User getUserFromToken(String token) {
        DecodedJWT result = JWT.require(Algorithm.HMAC512(SIGN_KEY)).build().verify(token);

        return USER_SERVICE.getById(Long.parseLong(result.getSubject()));
    }
}
