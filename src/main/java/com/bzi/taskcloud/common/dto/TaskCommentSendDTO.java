package com.bzi.taskcloud.common.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class TaskCommentSendDTO {
    @NotNull(message = "要评论的任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "评论内容不能为空")
    private String comment;

    @NotNull(message = "评分不能为空")
    private Float rating;
}
