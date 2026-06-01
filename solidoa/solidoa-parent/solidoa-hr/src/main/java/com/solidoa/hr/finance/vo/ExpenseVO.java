package com.solidoa.hr.finance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExpenseVO {
    private Long id;
    private String expenseNo;
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private String expenseType;
    private String expenseTypeName;
    private BigDecimal amount;
    private String reason;
    private String attachments;
    private String status;
    private String statusName;
    private LocalDateTime createTime;
}