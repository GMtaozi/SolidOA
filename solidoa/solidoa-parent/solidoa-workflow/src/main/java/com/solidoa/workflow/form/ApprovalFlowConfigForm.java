package com.solidoa.workflow.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 审批流程配置表单
 */
@Data
public class ApprovalFlowConfigForm {

    /** 业务类型 */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /** 流程名称 */
    @NotBlank(message = "流程名称不能为空")
    private String flowName;

    /** 是否默认流程 */
    private Boolean isDefault;

    /** 节点配置列表 */
    @NotNull(message = "节点配置不能为空")
    private List<ApprovalNodeConfig> nodes;

    /** 抄送人配置列表 */
    private List<CcUserConfig> ccUsers;

    /**
     * 审批节点配置
     */
    @Data
    public static class ApprovalNodeConfig {
        /** 节点顺序 */
        @NotNull(message = "节点顺序不能为空")
        private Integer order;

        /** 节点名称 */
        @NotBlank(message = "节点名称不能为空")
        private String name;

        /** 审批人类型: FIXED指定人员 / DIRECT_MANAGER直属上级 / ROLE角色 */
        @NotBlank(message = "审批人类型不能为空")
        private String approverType;

        /** 审批人ID（FIXED类型时使用） */
        private Long approverId;

        /** 审批人名称 */
        private String approverName;

        /** 角色编码（ROLE类型时使用） */
        private String roleCode;

        /** 审批模式: ALL会签 / ANY或签 */
        @NotBlank(message = "审批模式不能为空")
        private String mode;

        /**
         * 校验：FIXED类型时approverId不能为空，ROLE类型时roleCode不能为空
         */
        @AssertTrue(message = "指定人员时审批人ID不能为空，角色类型时角色编码不能为空")
        public boolean isApproverConfigValid() {
            if ("FIXED".equals(approverType)) {
                return approverId != null;
            } else if ("ROLE".equals(approverType)) {
                return roleCode != null && !roleCode.isBlank();
            }
            return true;
        }
    }

    /**
     * 抄送人配置
     */
    @Data
    public static class CcUserConfig {
        /** 抄送人类型: FIXED指定人员 / DIRECT_MANAGER直属上级 / DEPARTMENT_ADMIN部门管理员 */
        @NotBlank(message = "抄送人类型不能为空")
        private String type;

        /** 抄送人ID（FIXED类型时使用） */
        private Long userId;

        /** 抄送人名称 */
        private String userName;
    }
}
