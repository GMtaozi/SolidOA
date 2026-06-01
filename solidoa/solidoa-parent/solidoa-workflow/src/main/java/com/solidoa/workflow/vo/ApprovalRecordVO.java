package com.solidoa.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 审批记录VO
 */
@Data
public class ApprovalRecordVO {

    private Long id;

    /** 记录编号 */
    private String recordNo;

    /** 业务类型 */
    private String businessType;

    /** 业务类型名称 */
    private String businessTypeName;

    /** 业务ID */
    private Long businessId;

    /** 申请人ID */
    private Long userId;

    /** 申请人姓名 */
    private String userName;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 标题/摘要 */
    private String title;

    /** 金额 */
    private BigDecimal amount;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 状态 */
    private String status;

    /** 状态名称 */
    private String statusName;

    /** 审批类型: APPROVE/REJECT */
    private String approveType;

    /** 审批类型描述 */
    private String approveTypeDesc;

    /** 审批意见 */
    private String comment;

    /** 当前节点 */
    private String currentNode;

    /** 总节点数 */
    private Integer totalNodes;

    /** 已完成节点数 */
    private Integer completedNodes;

    /** 进度 */
    private String progress;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}