package com.solidoa.hr.finance.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetForm {
    private Long id;

    @NotNull(message = "部门ID不能为空")
    private Long deptId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份最小为1")
    private Integer month;

    @NotNull(message = "预算金额不能为空")
    @Min(value = 0, message = "预算金额不能为负数")
    private BigDecimal totalAmount;

    private BigDecimal warningThreshold;
}