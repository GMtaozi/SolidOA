package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批节点实体（运行时实例）
 */
@Data
@TableName("oa_approval_node")
public class ApprovalNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 节点顺序 */
    private Integer nodeOrder;

    /** 节点类型: APPROVER审批人 / CC抄送人 */
    private String nodeType;

    /** 审批模式: ALL会签 / ANY或签 */
    private String nodeMode;

    /** 审批人ID */
    private Long approverId;

    /** 审批人名称 */
    private String approverName;

    /** 状态: PENDING待处理 / APPROVED已通过 / REJECTED已拒绝 */
    private String status;

    /** 审批时间 */
    private LocalDateTime approvedTime;

    /** 审批意见 */
    private String comment;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
