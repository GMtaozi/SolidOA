package com.solidoa.workflow.dto;

import lombok.Data;

/**
 * 流程图节点
 */
@Data
public class GraphNode {
    /** 节点 ID（业务侧节点 ID） */
    private Long id;
    /** 节点顺序 */
    private Integer order;
    /** 节点名称（"直属领导审批"） */
    private String name;
    /** 节点类型：APPROVER / CC */
    private String type;
    /** 审批/抄送模式：ALL (会签) / ANY (或签) */
    private String mode;
    /** 审批人 ID */
    private Long approverId;
    /** 审批人姓名 */
    private String approverName;
    /** 状态：PENDING / APPROVED / REJECTED */
    private String status;
    /** 审批时间 */
    private String approvedTime;
    /** 审批意见 */
    private String comment;
    /** 父节点 ID（用于加签层级） */
    private Long parentNodeId;
}
