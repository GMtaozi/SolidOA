package com.solidoa.hr.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 差旅报销实体
 */
@Data
@TableName("oa_expense")
public class Expense {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报销单号 */
    private String expenseNo;

    /** 申请人ID */
    private Long userId;

    /** 申请人姓名(冗余字段，避免查询时关联用户表) */
    private String userName;

    /** 部门ID */
    private Long deptId;

    /** 部门名称(冗余字段，避免查询时关联部门表) */
    private String deptName;

    /** 报销类型: BUSINESS_TRIP出差, DAILY日常, MEETING会议, OTHER其他 */
    private String expenseType;

    /** 报销金额 */
    private BigDecimal amount;

    /** 收款账户 */
    private String bankAccount;

    /** 收款户名 */
    private String accountName;

    /** 开户行 */
    private String bankName;

    /** 事由说明 */
    private String reason;

    /** 附件 */
    private String attachments;

    /** 状态 */
    private String status;

    /** 流程实例ID */
    private String processInstanceId;

    /** 审批意见 */
    private String approverComment;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private LocalDateTime approvedTime;

    /** 支付时间 */
    private LocalDateTime paymentTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}