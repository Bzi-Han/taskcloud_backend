package com.bzi.taskcloud.common.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class TaskPublishDTO {
    @NotBlank(message = "任务名称不能为空")
    @Length(max = 256, message = "任务名称过长")
    private String name;

    @NotBlank(message = "任务描述不能为空")
    @Length(max = 1024, message = "任务描述过长")
    private String description;

    @Length(max = 256, message = "注意事项内容过长")
    private String warning;

    @NotNull(message = "任务使用的语言类型不能为空")
    @Range(min = 0, max = 2, message = "任务使用的语言类型不正确")
    private Integer type;

    @NotBlank(message = "任务的脚本不能为空")
    private String script;

    @Length(max = 16, message = "任务的版本信息过长")
    private String version;
}
