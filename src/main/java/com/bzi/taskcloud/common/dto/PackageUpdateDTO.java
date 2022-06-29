package com.bzi.taskcloud.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class PackageUpdateDTO extends PackageAddDTO {
    @NotNull(message = "任务ID不能为空")
    private Long id;

    @NotBlank(message = "任务配置不能为空")
    private String tasksConfig;
}
