package com.solidoa.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考勤同步日志表
 */
@Data
@TableName("oa_attendance_sync_log")
public class AttendanceSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 同步类型: CLOCK打卡/OVERTIME加班/APPROVAL审批 */
    private String syncType;

    /** 同步开始时间 */
    private LocalDateTime syncStartTime;

    /** 同步结束时间 */
    private LocalDateTime syncEndTime;

    /** 状态: SUCCESS/FAILED/RUNNING */
    private String status;

    /** 同步记录数 */
    private Integer recordsCount;

    /** 错误信息 */
    private String errorMessage;

    /** 同步参数(JSON) */
    private String syncParams;

    /** 重试次数 */
    private Integer retryCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}