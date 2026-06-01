package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工假期余额实体
 */
@Data
@TableName("oa_leave_balance")
public class LeaveBalance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 假期类型 */
    private String leaveType;

    /** 年度 */
    private Integer year;

    /** 可用天数 */
    private BigDecimal totalDays;

    /** 已使用天数 */
    private BigDecimal usedDays;

    /** 待审批天数 */
    private BigDecimal pendingDays;

    /** 已结转天数 */
    private BigDecimal transferredDays;

    /** 乐观锁版本号 */
    @Version
    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}