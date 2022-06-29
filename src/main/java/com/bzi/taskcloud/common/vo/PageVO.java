package com.bzi.taskcloud.common.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {
    private List<T> records;

    private long total;

    private long size;

    private long current;

    private long pages;
}
