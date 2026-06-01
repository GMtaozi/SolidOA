package com.solidoa.workflow.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流程详情VO
 */
@Data
public class ApprovalFlowVO {

    /** 业务ID */
    private Long businessId;

    /** 业务类型 */
    private String businessType;

    /** 业务单号 */
    private String businessNo;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人名称 */
    private String applicantName;

    /** 当前节点 */
    private CurrentNodeVO currentNode;

    /** 总节点数 */
    private Integer totalNodes;

    /** 已完成节点数 */
    private Integer completedNodes;

    /** 节点列表 */
    private List<NodeVO> nodes;

    /** 抄送人数量 */
    private Integer ccCount;

    /** 抄送人列表 */
    private List<CcVO> ccUsers;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 当前节点信息
     */
    @Data
    public static class CurrentNodeVO {
        private Integer order;
        private String name;
        private String approverName;
        private String mode;
    }

    /**
     * 节点信息
     */
    @Data
    public static class NodeVO {
        private Integer order;
        private String name;
        private String nodeType;
        private String mode;
        private String approverName;
        private String status;
        private LocalDateTime approvedTime;
        private String comment;
    }

    /**
     * 抄送人信息
     */
    @Data
    public static class CcVO {
        private Long userId;
        private String userName;
        private Boolean isRead;
        private LocalDateTime readTime;
    }
}
