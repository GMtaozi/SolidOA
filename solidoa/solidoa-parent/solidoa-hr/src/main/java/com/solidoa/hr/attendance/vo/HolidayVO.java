package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 节假日VO
 */
@Data
public class HolidayVO {

    private Long id;

    /**
     * 节假日日期
     */
    private LocalDate holidayDate;

    /**
     * 节假日名称
     */
    private String holidayName;

    /**
     * 类型
     */
    private String holidayType;

    /**
     * 类型名称
     */
    private String holidayTypeName;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}