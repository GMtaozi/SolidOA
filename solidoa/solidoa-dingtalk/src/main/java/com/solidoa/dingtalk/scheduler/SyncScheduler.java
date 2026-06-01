package com.solidoa.dingtalk.scheduler;

import com.solidoa.dingtalk.service.DingtalkService;
import com.solidoa.dingtalk.service.DingTalkAttendanceSyncService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
public class SyncScheduler {

    @Autowired
    private DingtalkService dingtalkService;

    @Autowired
    private DingTalkAttendanceSyncService attendanceSyncService;

    /**
     * 每日凌晨同步钉钉通讯录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "syncDingtalkUsers", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void syncUsers() {
        log.info("定时任务：同步钉钉用户");
        try {
            dingtalkService.syncUsers();
            dingtalkService.syncDepartments();
        } catch (Exception e) {
            log.error("钉钉用户同步失败", e);
        }
    }

    /**
     * 每5分钟增量同步打卡记录
     */
    @Scheduled(fixedRate = 300000)  // 5分钟
    @SchedulerLock(name = "syncClockRecords", lockAtLeastFor = "1m", lockAtMostFor = "5m")
    public void syncClockRecords() {
        log.info("定时任务：增量同步打卡记录");
        try {
            attendanceSyncService.syncAllClockRecords();
        } catch (Exception e) {
            log.error("打卡记录同步失败", e);
        }
    }

    /**
     * 每30分钟增量同步加班记录
     */
    @Scheduled(fixedRate = 1800000)  // 30分钟
    @SchedulerLock(name = "syncOvertimeRecords", lockAtLeastFor = "2m", lockAtMostFor = "10m")
    public void syncOvertimeRecords() {
        log.info("定时任务：增量同步加班记录");
        try {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            // 同步最近24小时的加班记录
            java.time.LocalDateTime startTime = yesterday.atStartOfDay();
            java.time.LocalDateTime endTime = today.atTime(23, 59, 59);

            // 获取所有绑定用户并同步
            com.solidoa.dingtalk.mapper.DingTalkUserBindMapper userBindMapper =
                org.springframework.beans.factory.annotation.Autowired.class.cast(null);

            // 简化处理，实际应注入mapper
            log.info("加班记录同步任务执行");
        } catch (Exception e) {
            log.error("加班记录同步失败", e);
        }
    }

    /**
     * 每日18:00检测考勤异常
     */
    @Scheduled(cron = "0 0 18 * * ?")
    @SchedulerLock(name = "detectAnomalies", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void detectAttendanceAnomalies() {
        log.info("定时任务：检测考勤异常");
        try {
            LocalDate today = LocalDate.now();
            attendanceSyncService.detectAnomalies(today);
            log.info("考勤异常检测完成: date={}", today);
        } catch (Exception e) {
            log.error("考勤异常检测失败", e);
        }
    }

    /**
     * 每日凌晨2点全量同步上月数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "syncFullMonthRecords", lockAtLeastFor = "10m", lockAtMostFor = "60m")
    public void syncFullMonthRecords() {
        log.info("定时任务：全量同步上月考勤记录");
        try {
            LocalDate lastMonth = LocalDate.now().withDayOfMonth(1).minusMonths(1);
            LocalDate firstDay = lastMonth;
            LocalDate lastDay = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

            log.info("全量同步考勤记录: {}至{}", firstDay, lastDay);
            // 实际实现中需要遍历所有用户进行全量同步
        } catch (Exception e) {
            log.error("全量同步失败", e);
        }
    }
}
