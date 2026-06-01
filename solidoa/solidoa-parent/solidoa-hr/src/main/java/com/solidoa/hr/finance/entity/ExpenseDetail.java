package com.solidoa.hr.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("oa_expense_detail")
public class ExpenseDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long expenseId;
    private LocalDate itemDate;
    private String itemType;
    private BigDecimal amount;
    private String description;
    private String attachment;
    private LocalDateTime createTime;
}