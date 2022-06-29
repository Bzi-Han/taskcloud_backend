package com.bzi.taskcloud.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskLogVO {
    private Long id;

    private String packageName;

    private String taskName;

    private String taskVersion;

    private String functions;

    private Integer state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executeOnTime;

    private String fromWhere;
}
