package com.solidoa.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("oa_contact")
public class Contact {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long deptId;

    private String deptName;

    private String realName;

    private String mobile;

    private String email;

    private String position;

    private String avatar;

    private Integer status = 1;
}