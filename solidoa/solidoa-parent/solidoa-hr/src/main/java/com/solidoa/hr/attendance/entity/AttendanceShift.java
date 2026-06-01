package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次配置实体
 */
@Data
@TableName("oa_attendance_shift")
public class AttendanceShift {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班次编码
     */
    private String shiftCode;

    /**
     * 班次名称
     */
    private String shiftName;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 午休开始时间
     */
    private LocalTime breakStart;

    /**
     * 午休结束时间
     */
    private LocalTime breakEnd;

    /**
     * 工作时长(小时)
     */
    private BigDecimal workHours;

    /**
     * 颜色
     */
    private String color;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态:0禁用,1启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}