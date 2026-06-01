package com.solidoa.dingtalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.dingtalk.dto.AttendanceCallbackDTO;
import com.solidoa.dingtalk.entity.*;
import com.solidoa.dingtalk.mapper.*;
import com.solidoa.dingtalk.service.DingTalkAttendanceSyncService;
import com.solidoa.dingtalk.vo.AttendanceStatisticsVO;
import com.solidoa.dingtalk.vo.SyncResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉考勤同步服务实现
 */
@Service
@Slf4j
public class DingTalkAttendanceSyncServiceImpl implements DingTalkAttendanceSyncService {

    @Autowired
    private DingTalkUserBindMapper userBindMapper;

    @Autowired
    private AttendanceSyncLogMapper syncLogMapper;

    @Autowired
    private AttendanceAnomalyMapper anomalyMapper;

    @Autowired
    private OvertimeExtMapper overtimeExtMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 限流配置
    private static final String RATE_LIMIT_KEY = "dingtalk:attendance:ratelimit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    @Override
    @Transactional
    public SyncResultVO syncClockRecords(Long userId, LocalDate startDate, LocalDate endDate) {
        long startTime = System.currentTimeMillis();
        log.info("开始同步打卡记录: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        // 1. 获取钉钉用户绑定
        DingTalkUserBind bind = userBindMapper.selectByUserId(userId);
        if (bind == null) {
            log.warn("用户未绑定钉钉: userId={}", userId);
            return SyncResultVO.failed("用户未绑定钉钉");
        }

        // 2. 记录同步日志
        AttendanceSyncLog syncLog = new AttendanceSyncLog();
        syncLog.setSyncType("CLOCK");
        syncLog.setSyncStartTime(LocalDateTime.now());
        syncLog.setSyncParams(String.format("{\"userId\":%d,\"startDate\":\"%s\",\"endDate\":\"%s\"}", userId, startDate, endDate));
        syncLog.setStatus("RUNNING");
        Long logId = syncLogMapper.insertSyncLog(syncLog);

        try {
            // 3. 检查限流
            String rateKey = RATE_LIMIT_KEY + bind.getDingtalkUserId();
            if (!checkRateLimit(rateKey)) {
                log.warn("触发限流: userId={}", userId);
                return SyncResultVO.failed("请求过于频繁，请稍后重试");
            }

            // 4. 模拟调用钉钉API获取打卡记录
            // 实际项目中需要调用钉钉考勤API
            List<Map<String, Object>> clockRecords = fetchClockRecordsFromDingTalk(
                bind.getDingtalkUserId(), startDate, endDate);

            // 5. 解析并存储
            int count = 0;
            for (Map<String, Object> record : clockRecords) {
                // 检查是否已存在
                String dingtalkRecordId = (String) record.get("id");
                if (dingtalkRecordId == null) continue;

                // 存储打卡数据（实际项目中存储到oa_attendance_record表）
                // 这里简化处理，仅记录日志
                count++;
            }

            // 6. 更新同步日志
            syncLogMapper.updateSuccess(logId, LocalDateTime.now(), count);

            long duration = System.currentTimeMillis() - startTime;
            log.info("同步打卡记录完成: userId={}, count={}, 耗时={}ms", userId, count, duration);

            return SyncResultVO.success(count);

        } catch (Exception e) {
            log.error("同步打卡记录失败: userId={}", userId, e);
            syncLogMapper.updateFailed(logId, LocalDateTime.now(), e.getMessage());
            return SyncResultVO.failed("同步失败: " + e.getMessage());
        }
    }

    @Override
    public SyncResultVO syncBatchClockRecords(List<Long> userIds, LocalDate date) {
        log.info("批量同步打卡记录: userIds={}, date={}", userIds.size(), date);
        int successCount = 0;
        int failCount = 0;

        for (Long userId : userIds) {
            try {
                SyncResultVO result = syncClockRecords(userId, date, date);
                if (result.getSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("批量同步失败: userId={}", userId, e);
                failCount++;
            }
        }

        log.info("批量同步完成: success={}, failed={}", successCount, failCount);
        return SyncResultVO.success(successCount);
    }

    @Override
    @Transactional
    public SyncResultVO syncOvertimeRecords(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("开始同步加班记录: userId={}, startTime={}, endTime={}", userId, startTime, endTime);

        DingTalkUserBind bind = userBindMapper.selectByUserId(userId);
        if (bind == null) {
            return SyncResultVO.failed("用户未绑定钉钉");
        }

        // 记录同步日志
        AttendanceSyncLog syncLog = new AttendanceSyncLog();
        syncLog.setSyncType("OVERTIME");
        syncLog.setSyncStartTime(LocalDateTime.now());
        syncLog.setSyncParams(String.format("{\"userId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}", userId, startTime, endTime));
        syncLog.setStatus("RUNNING");
        Long logId = syncLogMapper.insertSyncLog(syncLog);

        try {
            // 模拟调用钉钉API获取加班记录
            List<Map<String, Object>> overtimeRecords = fetchOvertimeRecordsFromDingTalk(
                bind.getDingtalkUserId(), startTime, endTime);

            int count = 0;
            for (Map<String, Object> record : overtimeRecords) {
                String dingtalkOvertimeId = (String) record.get("id");
                if (dingtalkOvertimeId == null) continue;

                // 检查是否已存在
                OvertimeExt existing = overtimeExtMapper.selectByDingtalkOvertimeId(dingtalkOvertimeId);
                if (existing != null) continue;

                // 存储加班数据
                OvertimeExt ext = new OvertimeExt();
                ext.setDingtalkOvertimeId(dingtalkOvertimeId);
                ext.setDurationHours(new BigDecimal(record.getOrDefault("duration", "0").toString()));
                ext.setOvertimeType((String) record.get("type"));
                ext.setDingtalkApproveResult((String) record.get("approveResult"));
                ext.setIsValid(true);
                overtimeExtMapper.insert(ext);
                count++;
            }

            syncLogMapper.updateSuccess(logId, LocalDateTime.now(), count);
            log.info("同步加班记录完成: userId={}, count={}", userId, count);

            return SyncResultVO.success(count);

        } catch (Exception e) {
            log.error("同步加班记录失败: userId={}", userId, e);
            syncLogMapper.updateFailed(logId, LocalDateTime.now(), e.getMessage());
            return SyncResultVO.failed("同步失败: " + e.getMessage());
        }
    }

    @Override
    public AttendanceStatisticsVO getStatistics(Long userId, String month) {
        log.info("获取考勤统计: userId={}, month={}", userId, month);

        AttendanceStatisticsVO vo = new AttendanceStatisticsVO();
        vo.setUserId(userId);
        vo.setMonth(month);

        // 获取当月异常统计
        List<AttendanceAnomaly> anomalies = anomalyMapper.selectByUserAndMonth(userId, month);

        int lateCount = 0, earlyCount = 0, absentCount = 0;
        for (AttendanceAnomaly anomaly : anomalies) {
            switch (anomaly.getAnomalyType()) {
                case "LATE": lateCount++; break;
                case "EARLY_LEAVE": earlyCount++; break;
                case "ABSENT": absentCount++; break;
            }
        }

        vo.setLateDays(lateCount);
        vo.setEarlyLeaveDays(earlyCount);
        vo.setAbsentDays(absentCount);

        // 计算正常天数
        LocalDate start = LocalDate.parse(month + "-01");
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        vo.setNormalDays(totalDays - lateCount - earlyCount - absentCount);

        return vo;
    }

    @Override
    @Transactional
    public void handleDingTalkCallback(AttendanceCallbackDTO callbackData) {
        log.info("处理钉钉考勤回调: type={}", callbackData.getMsgType());

        if ("checkin".equals(callbackData.getMsgType())) {
            for (AttendanceCallbackDTO.CheckinEvent event : callbackData.getCheckinList()) {
                processCheckinEvent(event);
            }
        }
    }

    @Override
    public void syncAllClockRecords() {
        log.info("同步所有员工打卡记录");
        List<Long> userIds = userBindMapper.selectAllActiveUserIds();
        LocalDate today = LocalDate.now();
        syncBatchClockRecords(userIds, today);
    }

    @Override
    @Transactional
    public void detectAnomalies(LocalDate date) {
        log.info("检测考勤异常: date={}", date);

        List<Long> userIds = userBindMapper.selectAllActiveUserIds();
        for (Long userId : userIds) {
            detectUserAnomaly(userId, date);
        }
    }

    /**
     * 处理打卡事件
     */
    private void processCheckinEvent(AttendanceCallbackDTO.CheckinEvent event) {
        log.info("处理打卡事件: userId={}, time={}, type={}",
            event.getUserId(), event.getCheckinTime(), event.getCheckinType());

        // 根据钉钉用户ID查找绑定
        DingTalkUserBind bind = userBindMapper.selectByDingtalkUserId(event.getUserId());
        if (bind == null) {
            log.warn("未找到钉钉用户绑定: dingtalkUserId={}", event.getUserId());
            return;
        }

        // 存储打卡数据（实际项目中存储到oa_attendance_record表）
        // 这里记录日志
        log.info("打卡数据已接收: userId={}, time={}, location={}",
            bind.getUserId(), event.getCheckinTime(), event.getLocationName());
    }

    /**
     * 检测用户考勤异常
     */
    private void detectUserAnomaly(Long userId, LocalDate date) {
        // 获取用户考勤配置（实际项目中从oa_attendance_config获取）
        LocalTime workStartTime = LocalTime.of(9, 0);
        LocalTime workEndTime = LocalTime.of(18, 0);
        int lateMinutesThreshold = 5;

        // 模拟获取打卡记录（实际项目中从oa_attendance_record查询）
        // 这里简化处理，假设没有打卡记录
        boolean hasClockIn = false;
        boolean hasClockOut = false;
        LocalTime actualClockIn = null;
        LocalTime actualClockOut = null;

        // 检测迟到
        if (!hasClockIn || (actualClockIn != null && actualClockIn.isAfter(workStartTime.plusMinutes(lateMinutesThreshold)))) {
            // 检查是否已记录
            int count = anomalyMapper.countByUserAndDateAndType(userId, date, "LATE");
            if (count == 0 && actualClockIn != null) {
                AttendanceAnomaly anomaly = new AttendanceAnomaly();
                anomaly.setUserId(userId);
                anomaly.setAnomalyType("LATE");
                anomaly.setAnomalyDate(date);
                long minutesLate = java.time.Duration.between(workStartTime, actualClockIn).toMinutes();
                anomaly.setDescription("迟到" + minutesLate + "分钟");
                anomaly.setSeverity(1);
                anomalyMapper.insertAnomaly(anomaly);
                log.info("检测到迟到异常: userId={}, minutes={}", userId, minutesLate);
            }
        }

        // 检测早退
        if (!hasClockOut || (actualClockOut != null && actualClockOut.isBefore(workEndTime.minusMinutes(lateMinutesThreshold)))) {
            int count = anomalyMapper.countByUserAndDateAndType(userId, date, "EARLY_LEAVE");
            if (count == 0 && actualClockOut != null) {
                AttendanceAnomaly anomaly = new AttendanceAnomaly();
                anomaly.setUserId(userId);
                anomaly.setAnomalyType("EARLY_LEAVE");
                anomaly.setAnomalyDate(date);
                long minutesEarly = java.time.Duration.between(actualClockOut, workEndTime).toMinutes();
                anomaly.setDescription("早退" + minutesEarly + "分钟");
                anomaly.setSeverity(1);
                anomalyMapper.insertAnomaly(anomaly);
                log.info("检测到早退异常: userId={}, minutes={}", userId, minutesEarly);
            }
        }

        // 检测缺卡
        if (!hasClockIn && !hasClockOut) {
            int count = anomalyMapper.countByUserAndDateAndType(userId, date, "ABSENT");
            if (count == 0) {
                AttendanceAnomaly anomaly = new AttendanceAnomaly();
                anomaly.setUserId(userId);
                anomaly.setAnomalyType("ABSENT");
                anomaly.setAnomalyDate(date);
                anomaly.setDescription("当日无打卡记录");
                anomaly.setSeverity(2);
                anomalyMapper.insertAnomaly(anomaly);
                log.info("检测到缺卡异常: userId={}", userId);
            }
        }
    }

    /**
     * 从钉钉获取打卡记录
     */
    private List<Map<String, Object>> fetchClockRecordsFromDingTalk(String dingtalkUserId, LocalDate startDate, LocalDate endDate) {
        // 模拟钉钉API返回数据
        // 实际项目中需要调用钉钉考勤API
        // API: taobao.attendance.getrecord
        log.info("调用钉钉打卡记录API: dingtalkUserId={}, startDate={}, endDate={}",
            dingtalkUserId, startDate, endDate);

        List<Map<String, Object>> records = new ArrayList<>();

        // 模拟返回数据
        LocalDateTime clockIn = startDate.atTime(9, 5);
        LocalDateTime clockOut = startDate.atTime(18, 10);

        Map<String, Object> record1 = new HashMap<>();
        record1.put("id", "dt_record_" + System.currentTimeMillis() + "_1");
        record1.put("userId", dingtalkUserId);
        record1.put("clockTime", clockIn.toString());
        record1.put("clockType", "OnDuty");
        record1.put("locationName", "公司");
        records.add(record1);

        Map<String, Object> record2 = new HashMap<>();
        record2.put("id", "dt_record_" + System.currentTimeMillis() + "_2");
        record2.put("userId", dingtalkUserId);
        record2.put("clockTime", clockOut.toString());
        record2.put("clockType", "OffDuty");
        record2.put("locationName", "公司");
        records.add(record2);

        return records;
    }

    /**
     * 从钉钉获取加班记录
     */
    private List<Map<String, Object>> fetchOvertimeRecordsFromDingTalk(String dingtalkUserId, LocalDateTime startTime, LocalDateTime endTime) {
        // 模拟钉钉API返回数据
        log.info("调用钉钉加班记录API: dingtalkUserId={}", dingtalkUserId);
        return new ArrayList<>();
    }

    /**
     * 限流检查
     */
    private boolean checkRateLimit(String key) {
        try {
            String count = redisTemplate.opsForValue().get(key);
            if (count != null && Integer.parseInt(count) >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
            return true;
        } catch (Exception e) {
            log.warn("限流检查失败: {}", e.getMessage());
            return true; // 失败时放行
        }
    }
}