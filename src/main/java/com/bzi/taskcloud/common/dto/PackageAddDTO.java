package com.bzi.taskcloud.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class PackageAddDTO {
    @NotBlank(message = "任务包名称不能为空")
    @Length(max = 32, message = "任务包名称过长")
    private String name;

    @NotNull(message = "任务是否激活不能为空")
    private Boolean activated;

    @NotNull(message = "任务是否每天运行不能为空")
    private Boolean runEveryday;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "任务定时时间不能为空")
    private LocalDateTime runOnTime;
}
