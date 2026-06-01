package com.solidoa.system.form;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserForm {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
    @Email(message = "邮箱格式不正确")
    private String email;
    private Long deptId;
    // status 字段仅允许管理员通过后端接口设置，创建和更新时忽略前端传入的 status
    // 如需允许前端设置，需在 Controller 层校验权限
    private Integer status;
}