package com.solidoa.hr.attendance.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 审批表单
 */
@Data
public class ApproveForm {
    @NotBlank(message = "审批意见不能为空")
    private String comment;

    /** 审批类型: APPROVE同意, REJECT拒绝 */
    @NotBlank(message = "审批类型不能为空")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "审批类型只能是APPROVE或REJECT")
    private String approveType;

    /** 调休时长（审批通过时可调整） */
    private java.math.BigDecimal approvedHours;
}