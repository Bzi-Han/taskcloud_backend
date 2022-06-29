package com.bzi.taskcloud.common.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class UserLoginDTO {
    @NotBlank(message = "账户名不能为空")
    @Email(message = "账户必须是邮箱")
    @Length(min = 5, max = 32, message = "账户格式不正确")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Length(min = 40, max = 40, message = "密码格式不正确")
    private String password;
}
