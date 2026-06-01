package com.solidoa.system.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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
    /** 用户角色列表 */
    private List<String> roles;
}