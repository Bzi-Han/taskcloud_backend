package com.bzi.taskcloud.security.auth;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.lang.UserState;
import com.bzi.taskcloud.common.lang.UserType;
import com.bzi.taskcloud.common.utils.JwtUtil;
import com.bzi.taskcloud.common.utils.LoggerUtil;
import com.bzi.taskcloud.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

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
        // 检测当前请求是否存在JwtToken
        if (StringUtils.isNotBlank(request.getHeader("Authorization"))) {
            User user = null;

            // 通过JwtToken获取用户信息
            try {
                user = JwtUtil.getUserFromToken(request.getHeader("Authorization"));
            } catch (Exception e) {
                LoggerUtil.failed(e.getMessage());
            }

            if (null != user) {
                // 检测用户是否已注销
                if (UserState.delete.ordinal() == user.getState()) {
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(400);
                    response.getWriter().write(new ObjectMapper().writeValueAsString(
                            Result.failed("不存在的账户。")
                    ));

                    return;
                }
                // 检测用户是否已被冻结
                if (UserState.freeze.ordinal() == user.getState()) {
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(400);
                    response.getWriter().write(new ObjectMapper().writeValueAsString(
                            Result.failed("您的账户已被冻结，如有需要请联系管理员！")
                    ));

                    return;
                }

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

        // 继续执行过滤器
        filterChain.doFilter(request, response);
    }
}
