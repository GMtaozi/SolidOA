package com.solidoa.workflow.form;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 审批操作表单
 */
@Data
public class ApproveForm {

    /** 审批操作: APPROVE-同意, REJECT-拒绝 */
    @NotBlank(message = "请选择审批操作")
    private String approveType;

    /** 审批意见 */
    private String comment;
}