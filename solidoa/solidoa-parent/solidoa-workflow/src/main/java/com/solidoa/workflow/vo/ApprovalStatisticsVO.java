package com.solidoa.workflow.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审批统计VO
 */
@Data
public class ApprovalStatisticsVO {

    /** 总申请数 */
    private Long totalCount;

    /** 待审批数 */
    private Long pendingCount;

    /** 已通过数 */
    private Long approvedCount;

    /** 已拒绝数 */
    private Long rejectedCount;

    /** 已撤回数 */
    private Long cancelledCount;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 按业务类型统计 */
    private Map<String, Long> byBusinessType;

    /** 按部门统计 */
    private Map<String, Long> byDept;
}