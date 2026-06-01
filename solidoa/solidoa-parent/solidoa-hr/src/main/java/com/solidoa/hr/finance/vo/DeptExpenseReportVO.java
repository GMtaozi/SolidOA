package com.solidoa.hr.finance.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DeptExpenseReportVO {
    private Long deptId;
    private String deptName;
    private Integer yearMonth;
    private BigDecimal totalAmount;
    private Integer expenseCount;
    private BigDecimal avgAmount;
}