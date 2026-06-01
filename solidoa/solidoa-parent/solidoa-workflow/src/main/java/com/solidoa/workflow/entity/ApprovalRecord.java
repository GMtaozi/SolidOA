package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 审批记录实体(统一)
 */
@Data
@TableName("oa_approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型: LEAVE, EXPENSE, PURCHASE, SALARY, STAMP, OVERTIME, BUSINESS_TRIP, REPAIR_CARD, OUTING */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 审批人ID */
    private Long approverId;

    /** 审批类型: SUBMIT提交, APPROVE通过, REJECT拒绝, CANCEL撤回 */
    private String approveType;

    /** 审批意见 */
    private String comment;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ========== 以下字段仅用于VO/查询，不映射数据库 ==========

    /** 记录编号 */
    @TableField(exist = false)
    private String recordNo;

    /** 申请人ID */
    @TableField(exist = false)
    private Long userId;

    /** 申请人姓名 */
    @TableField(exist = false)
    private String userName;

    /** 部门ID */
    @TableField(exist = false)
    private Long deptId;

    /** 部门名称 */
    @TableField(exist = false)
    private String deptName;

    /** 标题/摘要 */
    @TableField(exist = false)
    private String title;

    /** 申请内容摘要(JSON) */
    @TableField(exist = false)
    private String content;

    /** 金额(如有) */
    @TableField(exist = false)
    private BigDecimal amount;

    /** 开始日期(如有) */
    @TableField(exist = false)
    private LocalDate startDate;

    /** 结束日期(如有) */
    @TableField(exist = false)
    private LocalDate endDate;

    /** 状态: PENDING审批中, APPROVED已通过, REJECTED已拒绝, CANCELLED已撤回 */
    @TableField(exist = false)
    private String status;

    /** 当前节点 */
    @TableField(exist = false)
    private String currentNode;

    /** 总节点数 */
    @TableField(exist = false)
    private Integer totalNodes;

    /** 已完成节点数 */
    @TableField(exist = false)
    private Integer completedNodes;

    /** 更新时间 */
    @TableField(exist = false)
    private LocalDateTime updateTime;
}
