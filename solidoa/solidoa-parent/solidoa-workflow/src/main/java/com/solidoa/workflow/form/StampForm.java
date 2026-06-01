package com.solidoa.workflow.form;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 用印申请表单
 */
@Data
public class StampForm {

    /** 用印部门ID */
    @NotNull(message = "请选择用印部门")
    private Long deptId;

    /** 用印类型: PUBLIC-公章, CONTRACT-合同章, FINANCIAL-财务章, LEGAL-法人章 */
    @NotBlank(message = "请选择用印类型")
    private String stampType;

    /** 文件名称 */
    @NotBlank(message = "请输入文件名称")
    private String documentName;

    /** 文件份数 */
    @NotNull(message = "请输入文件份数")
    @Min(value = 1, message = "文件份数至少为1")
    private Integer documentCount;

    /** 用印事由 */
    private String usage;

    /** 备注 */
    private String remark;

    /** 附件列表 */
    private List<String> attachments;
}