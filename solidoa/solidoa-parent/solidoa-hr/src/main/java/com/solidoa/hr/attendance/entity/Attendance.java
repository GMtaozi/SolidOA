package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("oa_attendance")
public class Attendance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate checkDate;
    private String checkType;
    private LocalDateTime checkTime;
    private String location;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String deviceType;
    private String deviceId;
    private String dingtalkCheckId;
    private Integer isLate;
    private Integer isEarlyLeave;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}