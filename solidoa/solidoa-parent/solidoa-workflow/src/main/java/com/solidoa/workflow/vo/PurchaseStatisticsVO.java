package com.solidoa.workflow.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 采购统计VO
 */
@Data
public class PurchaseStatisticsVO {

    private BigDecimal totalAmount;
    private Integer totalCount;
    private Integer pendingCount;
    private Integer approvedCount;
    private Integer rejectedCount;
    private Map<String, BigDecimal> typeAmountMap;
    private Map<String, Integer> typeCountMap;
}