package com.solidoa.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤异常表
 */
@Data
@TableName("oa_attendance_anomaly")
public class AttendanceAnomaly {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 异常类型: LATE迟到/EARLY_LEAVE早退/ABSENT缺卡/TRUANCY旷工 */
    private String anomalyType;

    /** 异常日期 */
    private LocalDate anomalyDate;

    /** 异常描述 */
    private String description;

    /** 严重程度: 1普通/2重要/3紧急 */
    private Integer severity;

    /** 处理状态: PENDING待处理/PROCESSED已处理/IGNORED已忽略 */
    private String status;

    /** 处理方式: SUPPLY补卡/LEAVE请假/OVERTIME转加班/IGNORE忽略 */
    private String processType;

    /** 处理原因 */
    private String processReason;

    /** 处理时间 */
    private LocalDateTime processTime;

    /** 处理人ID */
    private Long processUserId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}