package com.solidoa.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer status;
    /** 数据权限范围: 1全部 2本部门 3本部门及下级 4本人 5自定义 */
    private Integer dataScope;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}