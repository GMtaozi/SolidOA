package com.solidoa.hr.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_budget")
public class Budget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptId;
    private Integer year;
    private Integer month;
    private BigDecimal totalAmount;
    private BigDecimal usedAmount;
    @TableField(exist = false)
    private BigDecimal remainingAmount;
    private BigDecimal warningThreshold;
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}