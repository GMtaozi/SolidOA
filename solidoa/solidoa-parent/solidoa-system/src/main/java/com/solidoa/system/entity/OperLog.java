package com.solidoa.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class OperLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;
    private String businessType;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private String responseData;
    private Long userId;
    private String userName;
    private String ip;
    private String location;
    private String errorMsg;
    /** 操作状态（0-失败,1-成功） */
    private Integer status;
    private Long executeTime;
    private LocalDateTime createTime;
}