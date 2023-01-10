package com.bzi.taskcloud.common.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotNull;

@Data
public class HelpReviewDTO {
    @NotNull(message = "帮助文章ID不能为空")
    private Long id;

    @NotNull(message = "帮助文章状态不能为空")
    @Range(min = 0, max = 2, message = "任务状态错误")
    private Integer state;

    @Length(max = 512, message = "帮助文章状态信息过长")
    private String stateMessage;
}
