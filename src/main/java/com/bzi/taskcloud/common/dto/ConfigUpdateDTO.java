package com.bzi.taskcloud.common.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class ConfigUpdateDTO {
    @NotNull(message = "通行证配置ID不能为空")
    private Long id;

    private String passport;
}
