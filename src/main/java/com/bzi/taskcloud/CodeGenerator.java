package com.bzi.taskcloud;

import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;
import java.util.List;

public class CodeGenerator {
    private static final String DATASOURCE_URL = "jdbc:mysql://127.0.0.1:3888/taskcloud?useUnicode=true&useSSL=false&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
    private static final String DATASOURCE_USERNAME = "root";
    private static final String DATASOURCE_PASSWORD = "cvY:mAUbv8KLk.X";

    private static final String AUTHOR = "Bzi_Han";

    private static final String MODULE_PARENT = "com.bzi.taskcloud";
    private static final String MODULE_NAME = "";

    private static final List<String> TABLES = List.of(
            "user",
            "task",
            "package",
            "config",
            "help",
            "task_comment",
            "task_log"
    );

    public static void main(String[] args) {
        // 数据源
        DataSourceConfig.Builder dataSourceConfigBuilder = new DataSourceConfig.Builder(
                DATASOURCE_URL,
                DATASOURCE_USERNAME,
                DATASOURCE_PASSWORD
        );

        // 全局配置
        GlobalConfig.Builder globalConfigBuilder = new GlobalConfig.Builder();
        globalConfigBuilder.outputDir(System.getProperty("user.dir") + "/src/main/java");
        globalConfigBuilder.author(AUTHOR);
        globalConfigBuilder.disableOpenDir();
        globalConfigBuilder.enableSwagger();

        // 包配置
        PackageConfig.Builder packageConfigBuilder = new PackageConfig.Builder();
        packageConfigBuilder.parent(MODULE_PARENT);
        packageConfigBuilder.moduleName(MODULE_NAME);
        packageConfigBuilder.pathInfo(
                Collections.singletonMap(
                        OutputFile.xml,
                        System.getProperty("user.dir") + "/src/main/resources/mapper"
                )
        );

        // 策略配置
        StrategyConfig.Builder strategyConfigBuilder = new StrategyConfig.Builder();
        strategyConfigBuilder.addInclude(TABLES);
        strategyConfigBuilder.entityBuilder()
                .enableLombok()
                .enableRemoveIsPrefix();
        strategyConfigBuilder.controllerBuilder()
                .enableRestStyle();

        // 生成器
        AutoGenerator autoGenerator = new AutoGenerator(dataSourceConfigBuilder.build());
        autoGenerator.global(globalConfigBuilder.build());
        autoGenerator.packageInfo(packageConfigBuilder.build());
        autoGenerator.strategy(strategyConfigBuilder.build());

        // 生成
        autoGenerator.execute(new FreemarkerTemplateEngine());
    }
}
