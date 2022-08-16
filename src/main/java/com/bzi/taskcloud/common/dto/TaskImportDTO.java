package com.bzi.taskcloud.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TaskImportDTO {
    @NotBlank(message = "任务仓库链接不能为空")
    private String repository;

    private boolean review;
}
