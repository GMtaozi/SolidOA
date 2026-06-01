package com.solidoa.hr.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_budget_adjust")
public class BudgetAdjust {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long budgetId;
    private BigDecimal adjustAmount;
    private String adjustType; // ADD, BACK
    private String reason;
    private Long createBy;
    private LocalDateTime createTime;
}