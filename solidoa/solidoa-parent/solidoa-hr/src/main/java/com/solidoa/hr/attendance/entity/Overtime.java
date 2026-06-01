package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加班申请实体
 */
@Data
@TableName("oa_overtime")
public class Overtime {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 加班单号 */
    private String overtimeNo;

    /** 申请人ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 加班类型: WORKDAY, REST_DAY, HOLIDAY */
    private String overtimeType;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 加班时长(小时)，最小单位0.5小时 */
    private BigDecimal hours;

    /** 加班原因 */
    private String reason;

    /** 补偿方式: BREAK调休, SALARY加班费 */
    private String compensationType;

    /** 状态: PENDING待审批, APPROVED已通过, REJECTED已拒绝, COMPLETED已完成 */
    private String status;

    /** Camunda流程实例ID */
    private String processInstanceId;

    /** 当前审批人ID */
    private Long currentApproverId;

    /** 审批通过的时长 */
    private BigDecimal approvedHours;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}