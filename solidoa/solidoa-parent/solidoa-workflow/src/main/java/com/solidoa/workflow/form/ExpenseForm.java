package com.solidoa.workflow.form;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 差旅报销表单
 */
@Data
public class ExpenseForm {

    /** 报销类型 */
    @NotBlank(message = "请选择报销类型")
    private String expenseType;

    /** 出差报告标题 */
    private String tripReportTitle;

    /** 出差开始日期 */
    private LocalDate tripStartDate;

    /** 出差结束日期 */
    private LocalDate tripEndDate;

    /** 出差天数 */
    private BigDecimal tripDays;

    /** 交通费 */
    private BigDecimal transportFee;

    /** 住宿费 */
    private BigDecimal hotelFee;

    /** 其他费用 */
    private BigDecimal otherFee;

    /** 合计金额（可自动计算） */
    @DecimalMin(value = "0.01", message = "合计金额必须大于0")
    private BigDecimal totalAmount;

    /** 收款户名 */
    private String accountName;

    /** 收款账户 */
    @NotBlank(message = "请输入收款账户")
    private String bankAccount;

    /** 开户行 */
    private String bankName;

    /** 事由说明 */
    private String reason;

    /** 附件列表 */
    private String attachments;
}