package com.solidoa.workflow.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用印申请VO
 */
@Data
public class StampVO {

    private Long id;
    private String stampNo;

    /** 申请人信息 */
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;

    /** 用印信息 */
    private String stampType;
    private String stampTypeDesc;
    private String documentName;
    private Integer documentCount;
    private String stampUsage;
    private String remark;
    private List<String> attachmentUrls;

    /** 状态 */
    private String status;
    private String statusDesc;

    /** 审批信息 */
    private Long approverId;
    private String approverName;
    private Long currentApproverId;
    private String currentApproverName;

    /** 用印记录 */
    private LocalDateTime stampTime;
    private String receivedBy;
    private String receivedMobile;

    /** 时间 */
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 审批历史 */
    private List<ApprovalRecordVO> approvalRecords;

    /** 流程节点信息 */
    private List<ApprovalNodeVO> approvalNodes;
}