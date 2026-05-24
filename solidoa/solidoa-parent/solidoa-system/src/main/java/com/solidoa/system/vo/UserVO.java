package com.solidoa.system.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String mobile;
    private String email;
    private Long deptId;
    private String deptName;
    private Integer status;
    private LocalDateTime createTime;
}