package com.solidoa.dingtalk.vo;

import lombok.Data;
import java.util.List;

/**
 * 考勤统计VO
 */
@Data
public class AttendanceStatisticsVO {

    /** 用户ID */
    private Long userId;

    /** 统计月份 */
    private String month;

    /** 正常天数 */
    private Integer normalDays;

    /** 迟到次数 */
    private Integer lateDays;

    /** 早退次数 */
    private Integer earlyLeaveDays;

    /** 缺卡次数 */
    private Integer absentDays;

    /** 旷工天数 */
    private Integer truancyDays;

    /** 请假天数 */
    private Integer leaveDays;

    /** 加班总时长(小时) */
    private Double overtimeHours;

    /** 出勤率 */
    private Double attendanceRate;

    /** 打卡记录列表 */
    private List<ClockRecordVO> clockRecords;

    /** 加班记录列表 */
    private List<OvertimeRecordVO> overtimeRecords;

    @Data
    public static class ClockRecordVO {
        private Long id;
        private String clockTime;
        private String clockType;
        private String locationName;
        private String source;
        private String status;
    }

    @Data
    public static class OvertimeRecordVO {
        private Long id;
        private String date;
        private String startTime;
        private String endTime;
        private Double durationHours;
        private String overtimeType;
        private String status;
    }
}