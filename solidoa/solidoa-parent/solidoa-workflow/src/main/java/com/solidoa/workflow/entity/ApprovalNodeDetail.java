package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批节点明细实体
 */
@Data
@TableName("oa_approval_node_detail")
public class ApprovalNodeDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记录ID */
    private Long recordId;

    /** 节点顺序 */
    private Integer nodeOrder;

    /** 节点名称 */
    private String nodeName;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 状态: PENDING待处理, APPROVED已通过, REJECTED已拒绝 */
    private String nodeStatus;

    /** 审批类型: APPROVE同意, REJECT拒绝 */
    private String approveType;

    /** 审批意见 */
    private String comment;

    /** 操作时间 */
    private LocalDateTime actionTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}