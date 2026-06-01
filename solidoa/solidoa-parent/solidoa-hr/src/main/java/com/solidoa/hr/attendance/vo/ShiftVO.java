package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次VO
 */
@Data
public class ShiftVO {

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
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}