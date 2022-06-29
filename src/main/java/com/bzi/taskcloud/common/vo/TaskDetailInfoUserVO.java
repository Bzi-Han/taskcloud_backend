package com.bzi.taskcloud.common.vo;

import lombok.Data;

@Data
public class TaskDetailInfoUserVO {
    private Long id;

    private String name;

    private String description;

    private String warning;

    private String interfaces;

    private String domain;

    private String author;

    private Long authorId;

    private Integer type;

    private String version;

    private Float rating;
}
