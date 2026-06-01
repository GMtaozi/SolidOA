package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用印申请实体
 */
@Data
@TableName("oa_stamp")
public class Stamp {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用印单号 */
    private String stampNo;

    /** 申请人ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 用印类型 */
    private String stampType;

    /** 文件名称 */
    private String documentName;

    /** 文件份数 */
    private Integer documentCount;

    /** 用印事由 */
    @TableField("stamp_usage")
    private String stampUsage;

    /** 附件 */
    private String attachments;

    /**
     * 状态
     * PENDING-审批中, APPROVED-已同意, REJECTED-已拒绝,
     * COMPLETED-已完成, CANCELLED-已撤回
     */
    private String status;

    /** Camunda流程实例ID */
    private String processInstanceId;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 审批意见 */
    private String approveComment;

    /** 当前审批人ID */
    private Long currentApproverId;

    /** 用印时间 */
    private LocalDateTime stampTime;

    /** 领用人 */
    private String receivedBy;

    /** 领用人手机 */
    private String receivedMobile;

    /** 备注（非数据库字段） */
    @TableField(exist = false)
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}