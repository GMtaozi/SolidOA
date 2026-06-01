package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 假期类型实体
 */
@Data
@TableName("oa_leave_type")
public class LeaveType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 假期编码 */
    private String leaveCode;

    /** 假期名称 */
    private String leaveName;

    /** 默认天数 */
    private BigDecimal defaultDays;

    /** 最大天数(0=无限制) */
    private BigDecimal maxDays;

    /** 是否需要证明材料 */
    private Integer requiresProof;

    /** 是否可结转 */
    private Integer canTransfer;

    /** 可结转最大天数 */
    private BigDecimal transferMaxDays;

    /** 有效期(月),0=永久有效 */
    private Integer validMonths;

    /** 是否扣工资 */
    private Integer deductSalary;

    /** 排序 */
    private Integer sort;

    /** 状态: 0禁用, 1启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}