package com.bzi.taskcloud.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskUpdateDTO extends TaskPublishDTO {
    @NotNull(message = "任务ID不能为空")
    private Long id;
}
