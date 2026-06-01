package com.solidoa.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钉钉用户绑定表
 */
@Data
@TableName("oa_dingtalk_user_bind")
public class DingTalkUserBind {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** SolidOA用户ID */
    private Long userId;

    /** 钉钉用户ID */
    private String dingtalkUserId;

    /** 钉钉UnionID */
    private String dingtalkUnionId;

    /** 钉钉部门ID */
    private String departmentId;

    /** 是否有效 */
    private Boolean isActive;

    /** 绑定时间 */
    private LocalDateTime bindTime;

    /** 解绑时间 */
    private LocalDateTime unbindTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}