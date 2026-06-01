package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出差申请实体
 */
@Data
@TableName("oa_business_trip")
public class BusinessTrip {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出差单号 */
    private String tripNo;

    /** 申请人ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 出差目的地 */
    private String destination;

    /** 出差类型: LOCAL本地, SHORT短途, LONG长途 */
    private String tripType;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 出差天数 */
    private BigDecimal days;

    /** 出差事由 */
    private String reason;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 预算说明 */
    private String budgetRemark;

    /** 随行人员 */
    private String companions;

    /** 附件 */
    private String attachments;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系电话 */
    private String emergencyPhone;

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