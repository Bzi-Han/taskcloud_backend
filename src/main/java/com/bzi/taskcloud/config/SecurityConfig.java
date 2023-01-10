package com.bzi.taskcloud.config;

import com.bzi.taskcloud.common.exception.GlobalExceptionHandler;
import com.bzi.taskcloud.security.auth.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {
    private final GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    public SecurityConfig(GlobalExceptionHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .cors().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .exceptionHandling().authenticationEntryPoint(globalExceptionHandler).accessDeniedHandler(globalExceptionHandler).and()
            .authorizeHttpRequests()
            .requestMatchers(
                    HttpMethod.GET,
                    "/swagger-ui/**", // swagger-ui
                    "/swagger-resources/**", // swagger-resources
                    "/v3/api-docs" // swagger-api-json
            ).anonymous()
            .requestMatchers(
                    "/user/register", // 用户注册
                    "/user/login" // 用户登陆
            ).anonymous()
            .anyRequest().authenticated();

        http.addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}
