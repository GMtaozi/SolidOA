package com.solidoa.system.form;

import lombok.Data;

@Data
public class ContactForm {
    private Long userId;
    private Long deptId;
    private String deptName;
    private String realName;
    private String mobile;
    private String email;
    private String position;
    private String avatar;
}