package com.solidoa.workflow.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批节点VO
 */
@Data
public class ApprovalNodeVO {

    /** 节点ID */
    private Long id;

    /** 节点类型: START, APPROVAL, END */
    private String nodeType;

    /** 节点名称 */
    private String nodeName;

    /** 审批人 */
    private Long approverId;
    private String approverName;

    /** 节点状态: PENDING-待审批, APPROVED-已同意, REJECTED-已拒绝 */
    private String status;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 审批意见 */
    private String comment;

    /** 节点序号 */
    private Integer orderNum;
}