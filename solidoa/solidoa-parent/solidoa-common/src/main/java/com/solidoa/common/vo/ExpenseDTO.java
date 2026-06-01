package com.solidoa.common.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExpenseDTO {
    private Long id;
    private String expenseNo;
    private Long userId;
    private String userName;
    private String expenseType;
    private BigDecimal amount;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}
