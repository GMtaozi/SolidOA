package com.solidoa.hr.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("oa_salary")
public class Salary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String salaryNo;

    // 基本信息
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private LocalDate applyDate;
    private LocalDate payDate;
    private String payType;
    @TableField(exist = false)
    private String payTypeName;

    // 工资期间
    @TableField("salary_month")
    private String salaryMonth;

    // 汇总数据
    private Integer employeeCount;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalDeduction;
    private BigDecimal totalNetSalary;

    // 附件
    private String attachments;

    // 状态
    private String status;

    // 审批信息
    private Long approverId;
    private String approverName;
    private LocalDateTime approveTime;
    private String approveComment;

    // 发放信息
    private LocalDateTime paidTime;

    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}