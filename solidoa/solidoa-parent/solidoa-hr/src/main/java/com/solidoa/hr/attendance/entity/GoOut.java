package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 外出申请实体
 */
@Data
@TableName("oa_go_out")
public class GoOut {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 外出单号 */
    private String outNo;

    /** 申请人ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 外出日期 */
    private LocalDate outDate;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 外出地点 */
    private String destination;

    /** 外出事由 */
    private String reason;

    /** 附件 */
    private String attachments;

    /** 状态 */
    private String status;

    /** Camunda流程实例ID */
    private String processInstanceId;

    /** 当前审批人ID */
    private Long currentApproverId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}