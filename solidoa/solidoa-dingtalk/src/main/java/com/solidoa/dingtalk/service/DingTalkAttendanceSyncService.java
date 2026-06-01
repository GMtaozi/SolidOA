package com.solidoa.dingtalk.service;

import com.solidoa.dingtalk.dto.AttendanceCallbackDTO;
import com.solidoa.dingtalk.vo.AttendanceStatisticsVO;
import com.solidoa.dingtalk.vo.SyncResultVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 钉钉考勤同步服务接口
 */
public interface DingTalkAttendanceSyncService {

    /**
     * 同步员工打卡记录
     * @param userId 员工ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 同步结果
     */
    SyncResultVO syncClockRecords(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 批量同步员工打卡记录
     * @param userIds 员工ID列表
     * @param date 日期
     * @return 同步结果
     */
    SyncResultVO syncBatchClockRecords(List<Long> userIds, LocalDate date);

    /**
     * 同步员工加班记录
     * @param userId 员工ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 同步结果
     */
    SyncResultVO syncOvertimeRecords(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取考勤统计
     * @param userId 员工ID
     * @param month 月份 (格式: yyyy-MM)
     * @return 考勤统计
     */
    AttendanceStatisticsVO getStatistics(Long userId, String month);

    /**
     * 处理钉钉Webhook推送
     * @param callbackData 回调数据
     */
    void handleDingTalkCallback(AttendanceCallbackDTO callbackData);

    /**
     * 同步所有员工打卡记录
     */
    void syncAllClockRecords();

    /**
     * 检测考勤异常
     */
    void detectAnomalies(LocalDate date);
}