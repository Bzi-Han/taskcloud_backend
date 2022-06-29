package com.bzi.taskcloud.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskCommentDetailInfoVO {
    private Long id;

    private String author;

    private Long authorId;

    private String comment;

    private Float rating;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
}
