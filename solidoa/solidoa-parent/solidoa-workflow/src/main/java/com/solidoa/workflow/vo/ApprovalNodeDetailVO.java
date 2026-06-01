package com.solidoa.workflow.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批节点明细VO
 */
@Data
public class ApprovalNodeDetailVO {

    private Long id;

    /** 节点顺序 */
    private Integer nodeOrder;

    /** 节点名称 */
    private String nodeName;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 状态 */
    private String nodeStatus;

    /** 审批类型 */
    private String approveType;

    /** 审批意见 */
    private String comment;

    /** 操作时间 */
    private LocalDateTime actionTime;
}