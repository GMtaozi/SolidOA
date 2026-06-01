package com.solidoa.workflow.vo;

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
    private BigDecimal amount;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}