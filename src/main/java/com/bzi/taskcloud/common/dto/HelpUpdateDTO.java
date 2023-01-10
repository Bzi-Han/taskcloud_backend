package com.bzi.taskcloud.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class HelpUpdateDTO extends HelpPublishDTO {
    @NotNull(message = "帮助文章ID不能为空")
    private Long id;
}
