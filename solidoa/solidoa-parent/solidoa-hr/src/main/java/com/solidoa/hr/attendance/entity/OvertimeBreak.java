package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加班调休记录实体
 * 调休比例统一1:1，调休有效期6个月
 */
@Data
@TableName("oa_overtime_break")
public class OvertimeBreak {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 加班记录ID */
    private Long overtimeId;

    /** 可用调休时长 */
    private BigDecimal availableHours;

    /** 已使用时长 */
    private BigDecimal usedHours;

    /** 过期时间 */
    private LocalDateTime expiredTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}