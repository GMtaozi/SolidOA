package com.solidoa.system.vo;

import lombok.Data;

@Data
public class ContactVO {
    private Long id;
    private Long userId;
    private Long deptId;
    private String deptName;
    private String realName;
    private String mobile;
    private String email;
    private String position;
    private String avatar;
    private Integer status;
}