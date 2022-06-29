package com.bzi.taskcloud.config;

import com.bzi.taskcloud.common.exception.GlobalExceptionHandler;
import com.bzi.taskcloud.security.auth.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    GlobalExceptionHandler globalExceptionHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .cors().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .exceptionHandling().authenticationEntryPoint(globalExceptionHandler).accessDeniedHandler(globalExceptionHandler).and()
            .authorizeRequests()
            .antMatchers(
                    HttpMethod.GET,
                    "/swagger-ui/**", // swagger-ui
                    "/swagger-resources/**", // swagger-resources
                    "/v3/api-docs" // swagger-api-json
            ).anonymous()
            .antMatchers(
                    "/user/register", // 用户注册
                    "/user/login" // 用户登陆
            ).anonymous()
            .anyRequest().authenticated();

        http.addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);
    }


}
