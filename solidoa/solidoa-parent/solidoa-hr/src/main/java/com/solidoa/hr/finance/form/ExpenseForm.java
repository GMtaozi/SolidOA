package com.solidoa.hr.finance.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExpenseForm {
    @NotNull(message = "报销类型不能为空")
    private String expenseType;

    @NotNull(message = "报销金额不能为空")
    @Positive(message = "报销金额必须为正数")
    private BigDecimal amount;

    private String reason;
    private String attachments;
    private String bankName;
    private String bankAccount;
}