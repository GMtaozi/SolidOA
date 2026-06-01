package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 抄送记录实体
 */
@Data
@TableName("oa_approval_cc")
public class ApprovalCc {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 抄送人ID */
    private Long ccUserId;

    /** 抄送人名称 */
    private String ccUserName;

    /** 通知状态 */
    private String notifyStatus;

    /** 是否已读 */
    private Boolean isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
