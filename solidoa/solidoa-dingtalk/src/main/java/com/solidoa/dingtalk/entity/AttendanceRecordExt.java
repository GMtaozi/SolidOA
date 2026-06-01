package com.solidoa.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考勤记录钉钉扩展表
 */
@Data
@TableName("oa_attendance_record_ext")
public class AttendanceRecordExt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考勤记录ID(关联oa_attendance_record) */
    private Long recordId;

    /** 钉钉打卡记录ID */
    private String dingtalkRecordId;

    /** 钉钉设备ID */
    private String dingtalkDeviceId;

    /** 定位结果: LOCATED定位成功/OUT_OF_RANGE超出范围/NORMAL正常 */
    private String locationResult;

    /** 打卡距离(米) */
    private BigDecimal workDistance;

    /** 基准打卡时间 */
    private LocalDateTime baseCheckTime;

    /** 是否超出打卡范围 */
    private Boolean outsideRange;

    /** 打卡经度 */
    private String positionLongitude;

    /** 打卡纬度 */
    private String positionLatitude;

    /** 定位精度(米) */
    private BigDecimal positionAccuracy;

    /** 数据来源: APP/PC/BEHALF代打卡 */
    private String dingtalkSource;

    /** 是否有效记录 */
    private Boolean isValid;

    private LocalDateTime createTime;
}