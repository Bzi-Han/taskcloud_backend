package com.bzi.taskcloud.common.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Data
public class UserUpdateInfoDTO {
    @NotBlank(message = "昵称不能为空")
    @Length(max = 32, message = "昵称格式不正确")
    private String nickname;
}
