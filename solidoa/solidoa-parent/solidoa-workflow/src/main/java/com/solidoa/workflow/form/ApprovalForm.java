package com.solidoa.workflow.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 审批操作表单
 */
@Data
public class ApprovalForm {

    /** 审批类型: APPROVE同意 / REJECT拒绝 */
    @NotBlank(message = "审批类型不能为空")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "审批类型只能是 APPROVE 或 REJECT")
    private String approveType;

    /** 审批意见 */
    private String comment;
}
