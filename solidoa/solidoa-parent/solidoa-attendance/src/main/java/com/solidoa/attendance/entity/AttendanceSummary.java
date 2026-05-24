package com.solidoa.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("oa_attendance_summary")
public class AttendanceSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long deptId;
    private String yearMonth;
    private Integer workDays;
    private Integer actualDays;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private java.math.BigDecimal absentDays;
    private java.math.BigDecimal leaveDays;
    private java.math.BigDecimal businessDays;
    private java.math.BigDecimal overtimeHours;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}