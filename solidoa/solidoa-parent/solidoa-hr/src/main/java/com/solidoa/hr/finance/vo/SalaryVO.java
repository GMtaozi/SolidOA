package com.solidoa.hr.finance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalaryVO {
    private Long id;
    private String salaryNo;
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private String yearMonth;

    // 工资项
    private BigDecimal baseSalary;
    private BigDecimal positionSalary;
    private BigDecimal performanceSalary;
    private BigDecimal overtimeSalary;
    private BigDecimal bonus;
    private BigDecimal subsidy;
    private BigDecimal otherAddition;

    // 扣款项
    private BigDecimal socialInsurance;
    private BigDecimal personalTax;
    private BigDecimal lateFine;
    private BigDecimal otherDeduction;

    // 计算结果
    private BigDecimal grossSalary;
    private BigDecimal netSalary;

    // 状态
    private String status;
    private String statusName;

    // 审批信息
    private Long creatorId;
    private String creatorName;
    private Long approverId;
    private String approverName;
    private LocalDateTime approveTime;
    private String approveComment;

    // 发放信息
    private LocalDateTime paidTime;

    private LocalDateTime createTime;
    private String createTimeStr;
}