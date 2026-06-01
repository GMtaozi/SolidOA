package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批流程配置实体
 */
@Data
@TableName("oa_approval_flow_config")
public class ApprovalFlowConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型: LEAVE, EXPENSE, STAMP, PURCHASE */
    private String businessType;

    /** 流程名称 */
    private String flowName;

    /** 是否默认流程 */
    private Boolean isDefault;

    /** 节点配置JSON */
    private String config;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
