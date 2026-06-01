package com.solidoa.system.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private String mobile;
    private String email;
    private Long deptId;
    private Integer status;
}