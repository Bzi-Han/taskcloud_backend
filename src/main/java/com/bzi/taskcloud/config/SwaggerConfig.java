package com.bzi.taskcloud.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@Configuration
@EnableSwagger2
@ConditionalOnProperty(prefix = "swagger2", value = {"enable"}, havingValue = "true")
public class SwaggerConfig {
    private static final ApiInfo API_INFO = new ApiInfoBuilder()
            .title("TaskCloud项目API文档")
            .description("云任务")
            .version("1.0.0")
            .build();

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(API_INFO)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.bzi.taskcloud.controller"))
                .paths(PathSelectors.any())
                .build();
    }
}