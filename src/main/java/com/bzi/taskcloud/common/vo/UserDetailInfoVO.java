package com.bzi.taskcloud.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDetailInfoVO {
    private Long id;

    private String nickname;

    private String username;

    private Integer type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registedTime;

    private Integer state;
}
