package com.bzi.taskcloud.security.auth;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.lang.UserState;
import com.bzi.taskcloud.common.lang.UserType;
import com.bzi.taskcloud.common.utils.JwtUtil;
import com.bzi.taskcloud.common.utils.LoggerUtil;
import com.bzi.taskcloud.entity.User;
import com.bzi.taskcloud.security.data.DataCrypto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JwtFilter extends OncePerRequestFilter {
    private final static Map<Integer, SimpleGrantedAuthority> USER_TYPE_MAPPING = Map.of(
            UserType.user.ordinal(), new SimpleGrantedAuthority("ROLE_user"),
            UserType.developer.ordinal(), new SimpleGrantedAuthority("ROLE_developer"),
            UserType.admin.ordinal(), new SimpleGrantedAuthority("ROLE_admin")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 检测当前请求是否存在JwtToken
            if (StringUtils.isNotBlank(request.getHeader("Authorization"))) {
                // 通过JwtToken获取用户信息
                User user = JwtUtil.getUserFromToken(request.getHeader("Authorization"));

                if (null != user) {
                    // 检测用户是否已注销
                    if (UserState.delete.ordinal() == user.getState())
                        throw new AccountNotFoundException("不存在的账户。");

                    // 检测用户是否已被冻结
                    if (UserState.freeze.ordinal() == user.getState())
                        throw new AccountLockedException("您的账户已被冻结，如有需要请联系管理员！");

                    // 设置登陆的用户信息
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    List.of(USER_TYPE_MAPPING.get(user.getType()))
                            )
                    );
                }
            }
        } catch (Exception e) {
            LoggerUtil.failed(e.getMessage());

            Result result;
            if (e instanceof TokenExpiredException)
                result = Result.make(401, "您的登陆已过期，请重新登陆！", null);
            else if (e instanceof AccountNotFoundException)
                result = Result.make(400, e.getMessage(), null);
            else if (e instanceof AccountLockedException)
                result = Result.make(403, e.getMessage(), null);
            else
                result = Result.failed("系统异常，请联系管理员！");

            var resultText = DataCrypto.encrypt(new ObjectMapper().writeValueAsString(
                    result
            ));

            response.setCharacterEncoding("UTF-8");
            response.setStatus(result.getCode());
            response.getWriter().write(resultText);

            return;
        }

        // 继续执行过滤器
        filterChain.doFilter(request, response);
    }
}
