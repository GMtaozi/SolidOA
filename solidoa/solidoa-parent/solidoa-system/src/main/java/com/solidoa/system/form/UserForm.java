package com.solidoa.system.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserForm {
    @NotBlank(message = "用户名不能为空")
    private String username;

    private String realName;
    private String mobile;
    private String email;
    private Long deptId;
    private Integer status;
}