package com.bzi.taskcloud.common.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class PackageAppendTaskDTO {
    @NotNull(message = "指定的任务包不能为空")
    private Long packageId;

    @NotNull(message = "指定的任务不能为空")
    private Long taskId;
}
