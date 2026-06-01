# SolidOA 钉钉考勤集成详细设计文档 (SSD)

> 版本：V1.0
> 日期：2026-05-26
> 状态：详细设计

---

## 一、系统架构

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              SolidOA 架构                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                  │
│  │   前端      │    │   网关      │    │   监控      │                  │
│  │  Vue3 Web   │◀──▶│  Gateway   │    │  Sentinel  │                  │
│  └─────────────┘    └─────────────┘    └─────────────┘                  │
│         │                  │                                            │
│         ▼                  ▼                                            │
│  ┌─────────────────────────────────────────────────────────────┐       │
│  │                      微服务层                                │       │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │       │
│  │  │ System   │  │Workflow  │  │  DingTalk│  │Attendance│  │       │
│  │  │  8081    │  │  8082    │  │  8087    │  │  8085    │  │       │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │       │
│  └─────────────────────────────────────────────────────────────┘       │
│         │                  │                    │                    │
│         ▼                  ▼                    ▼                    │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                │
│  │   MySQL     │    │   Redis     │    │  钉钉云API  │                │
│  │   数据库    │    │   缓存      │    │             │                │
│  └─────────────┘    └─────────────┘    └─────────────┘                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 考勤同步架构

```
┌───────────────────────────────────────────────────────────────────────┐
│                         钉钉考勤同步架构                               │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────┐          ┌──────────────┐          ┌──────────────┐ │
│  │   钉钉开放平台 │◀────────▶│ DingTalk服务 │◀────────▶│ Attendance服务│ │
│  │              │  API调用  │              │  数据传递 │              │ │
│  │  打卡Webhook │──────────│  同步任务    │──────────│  异常检测    │ │
│  └──────────────┘          └──────────────┘          └──────────────┘ │
│         │                         │                        │         │
│         │                         ▼                        ▼         │
│         │                 ┌──────────────┐          ┌──────────────┐  │
│         │                 │    Redis     │          │    MySQL     │  │
│         │                 │  同步缓存    │          │ oa_attendance │  │
│         │                 │  限流控制    │          │              │  │
│         │                 └──────────────┘          └──────────────┘  │
│         │                                                       │     │
│         │                         ┌──────────────────────────────┘     │
│         │                         │                                    │
│         │                         ▼                                    │
│         │                 ┌──────────────┐                            │
│         │                 │  消息通知    │                            │
│         └────────────────▶│ 钉钉推送/邮件 │                            │
│                           └──────────────┘                            │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 二、数据库设计

### 2.1 表结构设计

#### 2.1.1 考勤同步日志表

```sql
-- 考勤同步日志表
CREATE TABLE oa_attendance_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sync_type VARCHAR(20) NOT NULL COMMENT '同步类型:CLOCK打卡/OVERTIME加班/APPROVAL审批',
    sync_start_time DATETIME NOT NULL COMMENT '同步开始时间',
    sync_end_time DATETIME COMMENT '同步结束时间',
    status VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态:SUCCESS/FAILED/RUNNING',
    records_count INT DEFAULT 0 COMMENT '同步记录数',
    error_message TEXT COMMENT '错误信息',
    sync_params VARCHAR(500) COMMENT '同步参数(JSON)',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sync_type (sync_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤同步日志表';
```

#### 2.1.2 钉钉员工绑定表

```sql
-- 钉钉员工绑定表
CREATE TABLE oa_dingtalk_user_bind (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT 'SolidOA用户ID',
    dingtalk_user_id VARCHAR(64) NOT NULL COMMENT '钉钉用户ID',
    dingtalk_union_id VARCHAR(64) COMMENT '钉钉UnionID',
    department_id VARCHAR(64) COMMENT '钉钉部门ID',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否有效',
    bind_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    unbind_time DATETIME COMMENT '解绑时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_dingtalk_user_id (dingtalk_user_id),
    INDEX idx_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钉钉员工绑定表';

-- 考勤扩展表 - 记录钉钉打卡关联数据
CREATE TABLE oa_attendance_record_ext (
    id BIGINT PRIMARY KEY AUTO_increment COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '考勤记录ID(关联oa_attendance_record)',
    dingtalk_record_id VARCHAR(64) COMMENT '钉钉打卡记录ID',
    dingtalk_device_id VARCHAR(64) COMMENT '钉钉设备ID',
    location_result VARCHAR(20) COMMENT '定位结果:LOCATED定位成功/OUT_OF_RANGE超出范围/NORMAL正常',
    work_distance DECIMAL(10,2) COMMENT '打卡距离(米)',
    base_check_time DATETIME COMMENT '基准打卡时间',
    outside_range BOOLEAN DEFAULT FALSE COMMENT '是否超出打卡范围',
    position_longitude VARCHAR(20) COMMENT '打卡经度',
    position_latitude VARCHAR(20) COMMENT '打卡纬度',
    position_accuracy DECIMAL(10,2) COMMENT '定位精度(米)',
    dingtalk_source VARCHAR(20) COMMENT '数据来源:APP/PC/BEHALF代打卡',
    is_valid BOOLEAN DEFAULT TRUE COMMENT '是否有效记录',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record_id (record_id),
    INDEX idx_dingtalk_record_id (dingtalk_record_id),
    INDEX idx_dingtalk_source (dingtalk_source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录钉钉扩展表';
```

#### 2.1.3 加班记录扩展表

```sql
-- 加班记录扩展表
CREATE TABLE oa_overtime_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    overtime_id BIGINT COMMENT '加班记录ID(关联oa_overtime或oa_attendance)',
    dingtalk_overtime_id VARCHAR(64) COMMENT '钉钉加班记录ID',
    dingtalk_flow_id VARCHAR(64) COMMENT '钉钉审批流程ID',
    dingtalk_approve_result VARCHAR(20) COMMENT '钉钉审批结果',
    work_group_id VARCHAR(64) COMMENT '加班工作日组ID',
    work_group_name VARCHAR(100) COMMENT '加班工作日组名称',
    duration_hours DECIMAL(5,2) COMMENT '加班时长(小时)',
    overtime_type VARCHAR(20) COMMENT '加班类型:WD工作日/WE休息日/HOLIDAY节假日',
    converter_unit INT COMMENT '调休单位(小时)',
    is_valid BOOLEAN DEFAULT TRUE COMMENT '是否有效',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_overtime_id (overtime_id),
    INDEX idx_dingtalk_overtime_id (dingtalk_overtime_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班记录钉钉扩展表';
```

### 2.2 ER图

```
┌─────────────────────┐       ┌─────────────────────┐
│   oa_user           │       │  oa_attendance_record│
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ username            │       │ user_id (FK)         │
│ ...                 │◀──────│ clock_in_time       │
└─────────────────────┘       │ clock_out_time       │
        │                     │ status               │
        │                     └──────────┬──────────┘
        │                              │
        ▼                              │
┌─────────────────────┐       ┌────────┴───────────┐
│oa_dingtalk_user_bind│       │oa_attendance_record_ext│
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ user_id (FK)         │       │ record_id (FK)       │
│ dingtalk_user_id    │◀──────│ dingtalk_record_id   │
│ dingtalk_union_id    │       │ location_result      │
│ ...                 │       │ ...                  │
└─────────────────────┘       └─────────────────────┘

┌─────────────────────┐       ┌─────────────────────┐
│ oa_attendance_sync_log│     │    oa_overtime_ext  │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ sync_type           │       │ overtime_id (FK)    │
│ sync_start_time     │       │ dingtalk_overtime_id│
│ status              │       │ duration_hours       │
│ records_count       │       │ overtime_type       │
│ error_message       │       │ ...                  │
└─────────────────────┘       └─────────────────────┘
```

---

## 三、核心服务设计

### 3.1 钉钉考勤同步服务

#### 3.1.1 服务接口

```java
/**
 * 钉钉考勤同步服务
 */
public interface DingTalkAttendanceSyncService {

    /**
     * 同步员工打卡记录
     * @param userId 员工ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 同步结果
     */
    SyncResult syncClockRecords(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 批量同步员工打卡记录
     * @param userIds 员工ID列表
     * @param date 日期
     * @return 同步结果
     */
    SyncResult syncBatchClockRecords(List<Long> userIds, LocalDate date);

    /**
     * 同步员工加班记录
     * @param userId 员工ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 同步结果
     */
    SyncResult syncOvertimeRecords(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取考勤统计
     * @param userId 员工ID
     * @param month 月份 (格式: yyyy-MM)
     * @return 考勤统计
     */
    AttendanceStatistics getStatistics(Long userId, String month);

    /**
     * 处理钉钉Webhook推送
     * @param callbackData 回调数据
     */
    void handleDingTalkCallback(AttendanceCallbackDTO callbackData);
}
```

#### 3.1.2 打卡记录同步实现

```java
@Service
@Slf4j
public class DingTalkAttendanceSyncServiceImpl implements DingTalkAttendanceSyncService {

    @Autowired
    private DingTalkClient dingTalkClient;

    @Autowired
    private AttendanceRecordMapper attendanceMapper;

    @Autowired
    private AttendanceRecordExtMapper extMapper;

    @Autowired
    private DingTalkUserBindMapper bindMapper;

    @Autowired
    private AttendanceSyncLogMapper syncLogMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String SYNC_LOCK_KEY = "attendance:sync:clock:";
    private static final String SYNC_RATE_LIMIT_KEY = "attendance:sync:ratelimit:";

    @Override
    @Transactional
    public SyncResult syncClockRecords(Long userId, LocalDate startDate, LocalDate endDate) {
        long startTime = System.currentTimeMillis();
        SyncResult result = new SyncResult();

        try {
            // 1. 获取钉钉用户绑定
            DingTalkUserBind bind = bindMapper.selectByUserId(userId);
            if (bind == null) {
                result.setSuccess(false);
                result.setMessage("用户未绑定钉钉");
                return result;
            }

            // 2. 获取打卡数据
            List<DingTalkClockRecord> clockRecords = fetchClockRecordsFromDingTalk(
                bind.getDingtalkUserId(), startDate, endDate);

            // 3. 解析并存储
            int count = 0;
            for (DingTalkClockRecord record : clockRecords) {
                // 检查是否已存在
                if (extMapper.selectByDingtalkRecordId(record.getId()) != null) {
                    continue;
                }

                // 转换并存储
                AttendanceRecord attendance = convertToAttendanceRecord(record, userId);
                attendanceMapper.insert(attendance);

                // 存储扩展信息
                AttendanceRecordExt ext = convertToExt(record, attendance.getId());
                extMapper.insert(ext);

                count++;
            }

            // 4. 记录同步日志
            saveSyncLog("CLOCK", startDate, endDate, count, "SUCCESS", null);

            result.setSuccess(true);
            result.setCount(count);
            log.info("同步打卡记录成功: userId={}, count={}", userId, count);

        } catch (Exception e) {
            log.error("同步打卡记录失败: userId={}", userId, e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());

            // 记录错误日志
            saveSyncLog("CLOCK", startDate, endDate, 0, "FAILED", e.getMessage());

            throw new RuntimeException("同步打卡记录失败", e);
        }

        return result;
    }

    /**
     * 从钉钉获取打卡记录
     */
    private List<DingTalkClockRecord> fetchClockRecordsFromDingTalk(
        String dingtalkUserId, LocalDate startDate, LocalDate endDate) {

        // 调用钉钉API - 获取打卡记录
        // API: taobao.attendance.getrecord
        String response = dingTalkClient.execute("taobao.attendance.getrecord",
            Map.of(
                "userid", dingtalkUserId,
                "start_date", startDate.toString(),
                "end_date", endDate.toString(),
                "offset", "0",
                "limit", "100"
            )
        );

        // 解析返回数据
        return parseClockRecords(response);
    }

    /**
     * 限流控制
     */
    private boolean checkRateLimit(String key) {
        String count = redisTemplate.opsForValue().get(key);
        if (count != null && Integer.parseInt(count) >= 100) {
            return false;  // 超过限制
        }
        redisTemplate.opsForValue().increment(key);
        return true;
    }
}
```

### 3.2 考勤异常检测服务

```java
/**
 * 考勤异常检测服务
 */
@Service
@Slf4j
public class AttendanceAnomalyDetectionService {

    @Autowired
    private AttendanceRecordMapper attendanceMapper;

    @Autowired
    private AttendanceConfigMapper configMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private OvertimeService overtimeService;

    /**
     * 检测当日考勤异常
     * 每天18:00执行
     */
    @Scheduled(cron = "0 0 18 * * ?")
    public void detectDailyAnomalies() {
        LocalDate today = LocalDate.now();
        log.info("开始检测考勤异常: {}", today);

        // 获取所有员工
        List<Long> userIds = getAllActiveUserIds();

        for (Long userId : userIds) {
            detectUserDailyAnomaly(userId, today);
        }

        log.info("考勤异常检测完成: 检测用户数={}", userIds.size());
    }

    /**
     * 检测用户当日异常
     */
    public List<AttendanceAnomaly> detectUserDailyAnomaly(Long userId, LocalDate date) {
        List<AttendanceAnomaly> anomalies = new ArrayList<>();

        // 获取员工考勤配置
        AttendanceConfig config = getUserAttendanceConfig(userId);

        // 获取当日打卡记录
        AttendanceRecord record = attendanceMapper.selectByUserIdAndDate(userId, date);
        LocalTime workStartTime = config.getWorkStartTime();
        LocalTime workEndTime = config.getWorkEndTime();

        // 检测迟到
        if (record != null && record.getClockInTime() != null) {
            LocalTime actualInTime = record.getClockInTime().toLocalTime();
            if (actualInTime.isAfter(workStartTime.plusMinutes(config.getLateMinutes()))) {
                anomalies.add(AttendanceAnomaly.builder()
                    .userId(userId)
                    .anomalyType("LATE")
                    .anomalyDate(date)
                    .description(String.format("迟到%s分钟",
                        Duration.between(workStartTime, actualInTime).toMinutes()))
                    .severity(1)
                    .build());
            }
        }

        // 检测早退
        if (record != null && record.getClockOutTime() != null) {
            LocalTime actualOutTime = record.getClockOutTime().toLocalTime();
            if (actualOutTime.isBefore(workEndTime.minusMinutes(config.getEarlyMinutes()))) {
                anomalies.add(AttendanceAnomaly.builder()
                    .userId(userId)
                    .anomalyType("EARLY_LEAVE")
                    .anomalyDate(date)
                    .description(String.format("早退%s分钟",
                        Duration.between(actualOutTime, workEndTime).toMinutes()))
                    .severity(1)
                    .build());
            }
        }

        // 检测缺卡
        if (record == null) {
            anomalies.add(AttendanceAnomaly.builder()
                .userId(userId)
                .anomalyType("ABSENT")
                .anomalyDate(date)
                .description("当日无打卡记录")
                .severity(2)
                .build());
        }

        // 保存异常并发送通知
        for (AttendanceAnomaly anomaly : anomalies) {
            saveAndNotify(anomaly);
        }

        return anomalies;
    }

    /**
     * 发送异常通知
     */
    private void sendAnomalyNotification(AttendanceAnomaly anomaly) {
        // 发送钉钉消息
        messageService.sendDingTalkMessage(anomaly.getUserId(),
            String.format("【考勤提醒】您有一条考勤异常：%s，请及时处理。",
                anomaly.getDescription()));

        // 站内消息
        messageService.sendSystemMessage(anomaly.getUserId(),
            "考勤异常提醒", anomaly.getDescription());
    }
}
```

### 3.3 定时同步任务

```java
/**
 * 考勤同步定时任务
 */
@Component
@Slf4j
public class AttendanceSyncScheduler {

    @Autowired
    private DingTalkAttendanceSyncService syncService;

    @Autowired
    private DingTalkUserBindMapper bindMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 每5分钟增量同步打卡记录
     */
    @Scheduled(fixedRate = 300000)  // 5分钟
    public void syncClockRecordsIncrement() {
        log.info("开始增量同步打卡记录");
        LocalDate today = LocalDate.now();

        // 获取所有绑定钉钉的用户
        List<Long> userIds = bindMapper.selectAllActiveUserIds();

        for (Long userId : userIds) {
            try {
                syncService.syncClockRecords(userId, today, today);
            } catch (Exception e) {
                log.error("同步用户打卡记录失败: userId={}", userId, e);
            }
        }

        log.info("增量同步完成: 用户数={}", userIds.size());
    }

    /**
     * 每30分钟增量同步加班记录
     */
    @Scheduled(fixedRate = 1800000)  // 30分钟
    public void syncOvertimeRecordsIncrement() {
        log.info("开始增量同步加班记录");
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        List<Long> userIds = bindMapper.selectAllActiveUserIds();

        for (Long userId : userIds) {
            try {
                syncService.syncOvertimeRecords(userId, startTime, endTime);
            } catch (Exception e) {
                log.error("同步用户加班记录失败: userId={}", userId, e);
            }
        }

        log.info("加班记录同步完成: 用户数={}", userIds.size());
    }

    /**
     * 每日凌晨2点全量同步上月数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncFullMonthRecords() {
        log.info("开始全量同步上月考勤记录");
        LocalDate lastMonth = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        LocalDate firstDay = lastMonth;
        LocalDate lastDay = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

        List<Long> userIds = bindMapper.selectAllActiveUserIds();

        for (Long userId : userIds) {
            try {
                syncService.syncClockRecords(userId, firstDay, lastDay);
            } catch (Exception e) {
                log.error("全量同步用户打卡记录失败: userId={}", userId, e);
            }
        }

        log.info("全量同步完成: 用户数={}, 期间={}至{}", userIds.size(), firstDay, lastDay);
    }
}
```

---

## 四、API设计

### 4.1 后端API接口

#### 4.1.1 考勤数据同步

```java
/**
 * 钉钉考勤同步控制器
 */
@RestController
@RequestMapping("/api/v1/attendance/dingtalk")
@RequiredArgsConstructor
@Tag(name = "钉钉考勤同步", description = "钉钉考勤数据同步")
public class DingTalkAttendanceSyncController {

    @Autowired
    private DingTalkAttendanceSyncService syncService;

    @GetMapping("/sync/clock")
    @Operation(summary = "同步打卡记录")
    public Result<SyncResult> syncClockRecords(
        @RequestParam Long userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.success(syncService.syncClockRecords(userId, startDate, endDate));
    }

    @GetMapping("/sync/batch/clock")
    @Operation(summary = "批量同步打卡记录")
    public Result<SyncResult> syncBatchClockRecords(
        @RequestParam List<Long> userIds,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return Result.success(syncService.syncBatchClockRecords(userIds, date));
    }

    @GetMapping("/sync/overtime")
    @Operation(summary = "同步加班记录")
    public Result<SyncResult> syncOvertimeRecords(
        @RequestParam Long userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        return Result.success(syncService.syncOvertimeRecords(userId, startTime, endTime));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取考勤统计")
    public Result<AttendanceStatisticsVO> getStatistics(
        @RequestParam Long userId,
        @RequestParam String month
    ) {
        return Result.success(syncService.getStatistics(userId, month));
    }

    @PostMapping("/callback")
    @Operation(summary = "处理钉钉Webhook回调")
    public Result<Void> handleCallback(@RequestBody AttendanceCallbackDTO callbackData) {
        syncService.handleDingTalkCallback(callbackData);
        return Result.success();
    }
}
```

#### 4.1.2 考勤异常接口

```java
/**
 * 考勤异常控制器
 */
@RestController
@RequestMapping("/api/v1/attendance/anomaly")
@RequiredArgsConstructor
@Tag(name = "考勤异常", description = "考勤异常检测与处理")
public class AttendanceAnomalyController {

    @Autowired
    private AttendanceAnomalyDetectionService anomalyService;

    @GetMapping("/my")
    @Operation(summary = "获取我的考勤异常")
    public Result<List<AttendanceAnomalyVO>> getMyAnomalies(
        @RequestParam(required = false) String month
    ) {
        return Result.success(anomalyService.getMyAnomalies(getCurrentUserId(), month));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "处理考勤异常")
    public Result<Void> processAnomaly(
        @PathVariable Long id,
        @RequestBody AnomalyProcessForm form
    ) {
        anomalyService.processAnomaly(id, form.getProcessType(), form.getReason());
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取考勤异常统计")
    public Result<AnomalyStatisticsVO> getAnomalyStatistics(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.success(anomalyService.getStatistics(getCurrentUserId(), startDate, endDate));
    }
}
```

### 4.2 前端API

```javascript
// attendance.js
export const attendanceApi = {
  // 钉钉考勤同步
  syncClockRecords: (userId, startDate, endDate) =>
    api.get('/v1/attendance/dingtalk/sync/clock', { params: { userId, startDate, endDate } }),

  syncBatchClockRecords: (userIds, date) =>
    api.get('/v1/attendance/dingtalk/sync/batch/clock', { params: { userIds: userIds.join(','), date } }),

  syncOvertimeRecords: (userId, startTime, endTime) =>
    api.get('/v1/attendance/dingtalk/sync/overtime', { params: { userId, startTime, endTime } }),

  getDingTalkStatistics: (userId, month) =>
    api.get('/v1/attendance/dingtalk/statistics', { params: { userId, month } }),

  // 考勤异常
  getMyAnomalies: (month) =>
    api.get('/v1/attendance/anomaly/my', { params: { month } }),

  processAnomaly: (id, form) =>
    api.post(`/v1/attendance/anomaly/${id}/process`, form),

  getAnomalyStatistics: (startDate, endDate) =>
    api.get('/v1/attendance/anomaly/statistics', { params: { startDate, endDate } })
}
```

---

## 五、前端设计

### 5.1 考勤打卡页面增强

```vue
<template>
  <div class="attendance-page">
    <!-- 考勤概览 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ stats.normal }}</div>
        <div class="stat-label">正常</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-value">{{ stats.late }}</div>
        <div class="stat-label">迟到</div>
      </div>
      <div class="stat-card danger">
        <div class="stat-value">{{ stats.earlyLeave }}</div>
        <div class="stat-label">早退</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.overtime }}</div>
        <div class="stat-label">加班(小时)</div>
      </div>
    </div>

    <!-- 打卡记录(来自钉钉) -->
    <div class="section">
      <h3 class="section-title">打卡记录 <span class="sync-time">同步于 {{ lastSyncTime }}</span></h3>
      <div class="record-list">
        <div v-for="record in clockRecords" :key="record.id" class="record-item">
          <div class="record-date">{{ formatDate(record.clockTime) }}</div>
          <div class="record-detail">
            <span class="clock-time">{{ formatTime(record.clockTime) }}</span>
            <span class="clock-type" :class="record.clockType.toLowerCase()">
              {{ record.clockType === 'OnDuty' ? '上班' : '下班' }}
            </span>
            <span class="source" v-if="record.dingtalkSource">
              <el-icon><DingTalk /></el-icon> 钉钉
            </span>
          </div>
          <div class="record-status" :class="record.status.toLowerCase()">
            {{ record.status }}
          </div>
        </div>
      </div>
    </div>

    <!-- 加班记录(来自钉钉) -->
    <div class="section">
      <h3 class="section-title">加班记录 <span class="sync-time">同步于 {{ lastOvertimeSyncTime }}</span></h3>
      <div class="overtime-list">
        <div v-for="item in overtimeRecords" :key="item.id" class="overtime-item">
          <div class="overtime-date">{{ item.date }}</div>
          <div class="overtime-duration">{{ item.durationHours }}小时</div>
          <div class="overtime-type" :class="item.overtimeType.toLowerCase()">
            {{ getOvertimeTypeName(item.overtimeType) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { attendanceApi } from '@/api/attendance'
import { DingTalk } from '@element-plus/icons-vue'

const stats = ref({ normal: 0, late: 0, earlyLeave: 0, overtime: 0 })
const clockRecords = ref([])
const overtimeRecords = ref([])
const lastSyncTime = ref('')
const lastOvertimeSyncTime = ref('')

const loadData = async () => {
  // 加载统计数据
  const statRes = await attendanceApi.getDingTalkStatistics(userId, currentMonth)
  stats.value = statRes.data

  // 加载打卡记录
  const clockRes = await attendanceApi.getClockRecords(userId, startDate, endDate)
  clockRecords.value = clockRes.data

  // 加载加班记录
  const overtimeRes = await attendanceApi.getOvertimeRecords(userId, startTime, endTime)
  overtimeRecords.value = overtimeRes.data
}
</script>
```

### 5.2 考勤管理页面

```vue
<template>
  <div class="attendance-admin">
    <!-- 考勤概览 -->
    <div class="overview-cards">
      <div class="card">
        <h4>今日考勤</h4>
        <div class="stats">
          <span>出勤 {{ todayStats.present }} 人</span>
          <span>请假 {{ todayStats.absent }} 人</span>
          <span>迟到 {{ todayStats.late }} 人</span>
        </div>
      </div>
      <div class="card">
        <h4>异常提醒</h4>
        <div class="anomaly-list">
          <div v-for="a in anomalies" :key="a.id" class="anomaly-item">
            <span>{{ a.userName }}</span>
            <span class="type">{{ a.anomalyType }}</span>
            <span class="desc">{{ a.description }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 月度报表导出 -->
    <div class="export-section">
      <el-date-picker v-model="exportMonth" type="month" placeholder="选择月份" />
      <el-button type="primary" @click="exportReport">导出Excel</el-button>
      <el-button @click="exportPdf">导出PDF</el-button>
    </div>
  </div>
</template>
```

---

## 六、数据流设计

### 6.1 打卡数据同步流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  钉钉App │     │ 钉钉云   │     │ 同步服务 │     │  数据库  │
└────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
     │               │               │               │
     │ 员工打卡      │               │               │
     │──────────────▶│               │               │
     │               │               │               │
     │  webhook推送  │               │               │
     │◀──────────────│               │               │
     │               │               │               │
     │               │  定时拉取     │               │
     │               │─────────────▶│               │
     │               │               │               │
     │               │               │  解析+存储    │
     │               │               │─────────────▶│
     │               │               │               │
     │               │               │  异常检测    │
     │               │               │───────┐       │
     │               │               │       │       │
     │               │               │       ▼       │
     │               │               │  发送通知     │
     │               │               │               │
```

### 6.2 加班数据同步流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  钉钉App │     │ 钉钉云   │     │ 同步服务 │     │  数据库  │
└────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
     │               │               │               │
     │ 提交加班申请  │               │               │
     │──────────────▶│               │               │
     │               │               │               │
     │  审批通过    │               │               │
     │◀──────────────│               │               │
     │               │               │               │
     │               │  增量拉取     │               │
     │               │─────────────▶│               │
     │               │               │               │
     │               │               │  解析+存储    │
     │               │               │─────────────▶│
     │               │               │               │
     │               │               │  更新加班统计 │
     │               │               │───────┐       │
     │               │               │       ▼       │
     │               │               │  通知员工     │
     │               │               │               │
```

---

## 七、错误处理

### 7.1 异常场景处理

| 场景 | 处理方式 |
|------|----------|
| 钉钉API调用失败 | 记录日志，重试3次，间隔30秒 |
| 数据重复同步 | 检查recordId，已存在则跳过 |
| 用户未绑定钉钉 | 记录日志，标记为不同步 |
| 网络超时 | 重试3次，记录失败日志 |
| 同步数据量过大 | 分批处理，每批100条 |

### 7.2 重试机制

```java
@Service
@Slf4j
public class RetryableSyncService {

    @Retryable(
        value = { DingTalkApiException.class, NetworkException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 30000, multiplier = 2)
    )
    public SyncResult syncWithRetry(Long userId, LocalDate date) {
        // 重试逻辑
        return syncService.syncClockRecords(userId, date, date);
    }

    @Recover
    public SyncResult recover(Exception e, Long userId, LocalDate date) {
        log.error("同步失败，放弃重试: userId={}, date={}", userId, date, e);

        // 保存失败记录
        saveFailedSyncLog(userId, date, e.getMessage());

        return SyncResult.failed("同步失败: " + e.getMessage());
    }
}
```

---

## 八、测试计划

### 8.1 单元测试

| 测试项 | 测试内容 |
|--------|----------|
| 打卡记录同步 | 验证数据正确解析和存储 |
| 加班记录同步 | 验证加班类型和时长计算 |
| 异常检测 | 验证迟到、早退、缺卡检测 |
| 限流控制 | 验证Redis限流逻辑 |

### 8.2 集成测试

| 测试项 | 测试内容 |
|--------|----------|
| 钉钉API集成 | 验证API调用和响应解析 |
| 数据库操作 | 验证数据CRUD正确性 |
| 定时任务 | 验证定时任务触发和执行 |
| 消息通知 | 验证钉钉消息发送成功 |

### 8.3 性能测试

| 测试项 | 指标要求 |
|--------|----------|
| 单用户同步 | ≤ 3秒 |
| 批量同步100用户 | ≤ 30秒 |
| API并发压测 | 100并发无异常 |

---

## 九、部署配置

### 9.1 应用配置

```yaml
# solidoa-dingtalk/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/oa_dingtalk?useSSL=false&serverTimezone=Asia/Shanghai
    password: ${DB_PASSWORD}

dingtalk:
  app:
    app-key: ${DINGTALK_APP_KEY}
    app-secret: ${DINGTALK_APP_SECRET}
  attendance:
    sync:
      clock-cron: "0 */5 * * * ?"      # 每5分钟
      overtime-cron: "0 */30 * * * ?"   # 每30分钟
      full-cron: "0 0 2 * * ?"          # 每日2点
    callback:
      url: ${DINGTALK_CALLBACK_URL}/api/v1/attendance/dingtalk/callback

redis:
  host: ${REDIS_HOST}
  port: ${REDIS_PORT}
  password: ${REDIS_PASSWORD}
```

### 9.2 钉钉应用权限

需要申请以下权限：
- `attendance.getRecord` - 获取打卡记录
- `attendance.getOvertimeList` - 获取加班记录
- `attendance.getStatistics` - 获取考勤统计
- ` attendance.lists` - 获取考勤组列表

---

## 十、版本历史

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|----------|------|
| V1.0 | 2026-05-26 | 初始版本 | Claude |