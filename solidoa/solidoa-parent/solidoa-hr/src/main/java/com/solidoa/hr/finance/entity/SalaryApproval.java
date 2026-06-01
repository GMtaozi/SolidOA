package com.solidoa.hr.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工资审批实体
 */
@Data
@TableName("oa_salary_approval")
public class SalaryApproval {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工资单号 */
    private String salaryNo;

    /** 申请人ID */
    private Long userId;

    /** 申请人姓名 */
    private String userName;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 工资月份 */
    private String yearMonth;

    /** 基本工资 */
    private BigDecimal basicSalary;

    /** 岗位工资 */
    private BigDecimal positionSalary;

    /** 全勤奖 */
    private BigDecimal fullAttendanceBonus;

    /** 加班费 */
    private BigDecimal overtimePay;

    /** 扣款合计 */
    private BigDecimal deductions;

    /** 应发工资 */
    private BigDecimal grossSalary;

    /** 实发工资 */
    private BigDecimal netSalary;

    /** 审批意见 */
    private String approverComment;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private LocalDateTime approvedTime;

    /**
     * 状态
     * DRAFT-草稿, PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝
     */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}