package com.bzi.taskcloud.common.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class UserRegisterDTO {
    @Length(max = 32, message = "昵称格式不正确")
    private String nickname;

    @NotBlank(message = "账户名不能为空")
    @Email(message = "账户必须使用邮箱注册")
    @Length(min = 5, max = 32, message = "账户格式不正确")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Length(min = 40, max = 40, message = "密码格式不正确")
    private String password;

    @NotNull(message = "注册的用户类型不能为空")
    @Range(min = 0, max = 1, message = "不存在的用户类型")
    private int registerType;
}
