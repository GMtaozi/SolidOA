package com.solidoa.common.vo;

import lombok.Data;

@Data
public class UserSimpleVO {
    private Long id;
    private String username;
    private String realName;
    private Long deptId;
    private String deptName;
}