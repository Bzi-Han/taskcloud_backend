package com.bzi.taskcloud.common.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;

@Data
public class HelpPublishDTO {
    @NotBlank(message = "帮助文章标题不能为空")
    @Length(max = 256, message = "帮助文章标题过长")
    private String title;

    @NotBlank(message = "帮助文章内容不能为空")
    private String content;
}
