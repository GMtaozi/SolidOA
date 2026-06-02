package com.solidoa.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String type;  // menu, button, api
    private String url;
    private String method; // GET, POST, PUT, DELETE
    private Long parentId;
    /** 关联菜单ID (V2.0 新增) */
    private Long menuId;
    /** 前端路由路径 (V2.0 新增) */
    private String path;
    /** 前端组件路径 (V2.0 新增) */
    private String component;
    /** 是否隐藏菜单 (V2.0 新增) */
    private Integer hidden;
    private Integer sort;
    private String icon;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}