package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 节假日配置实体
 */
@Data
@TableName("oa_attendance_holiday")
public class AttendanceHoliday {

    @TableId(type = IdType.AUTO)
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
     * 类型:HOLIDAY节假日,WORKDAY调休上班
     */
    private String holidayType;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}