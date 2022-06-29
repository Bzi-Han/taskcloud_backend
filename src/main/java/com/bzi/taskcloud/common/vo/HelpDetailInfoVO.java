package com.bzi.taskcloud.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HelpDetailInfoVO {
    private Long id;

    private String title;

    private String author;

    private Long authorId;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
}
