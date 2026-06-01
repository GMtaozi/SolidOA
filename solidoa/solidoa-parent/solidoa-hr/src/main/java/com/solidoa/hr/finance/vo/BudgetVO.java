package com.solidoa.hr.finance.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetVO {
    private Long id;
    private Long deptId;
    private String deptName;
    private Integer year;
    private Integer month;
    private BigDecimal totalAmount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal warningThreshold;
    private Double usageRate;
}