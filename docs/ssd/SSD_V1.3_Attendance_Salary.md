# SolidOA 考勤与工资管理详细设计（SSD）

> 版本：V1.3
> 日期：2026-05-26
> 作者：SolidOA Architecture Team
> 状态：设计阶段

---

## 目录

1. [考勤规则详细设计](#1-考勤规则详细设计)
2. [加班模块设计](#2-加班模块设计)
3. [补卡模块设计](#3-补卡模块设计)
4. [出差模块设计](#4-出差模块设计)
5. [假期管理设计](#5-假期管理设计)
6. [工资管理设计](#6-工资管理设计)
7. [菜单结构设计](#7-菜单结构设计)
8. [审批记录功能设计](#8-审批记录功能设计)
9. [系统管理菜单设计](#9-系统管理菜单设计)

---

## 1. 考勤规则详细设计

### 1.1 考勤规则配置表

```sql
-- 考勤规则配置表
CREATE TABLE oa_attendance_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(20) NOT NULL COMMENT '规则类型:OVERTIME,LATE,EARLY_LEAVE,ABSENT,REPAIR',
    config JSON NOT NULL COMMENT '规则配置JSON',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 班次配置表
CREATE TABLE oa_attendance_shift (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_code VARCHAR(50) NOT NULL UNIQUE COMMENT '班次编码',
    shift_name VARCHAR(100) NOT NULL COMMENT '班次名称',
    check_in_time TIME NOT NULL COMMENT '上班时间',
    check_out_time TIME NOT NULL COMMENT '下班时间',
    flexible_minutes INT DEFAULT 0 COMMENT '弹性时间(分钟)',
    grace_minutes INT DEFAULT 0 COMMENT '宽限时间(分钟)',
    work_days VARCHAR(50) DEFAULT '1,2,3,4,5' COMMENT '工作日(1=周一到5=周五)',
    applicable_depts VARCHAR(500) COMMENT '适用部门(JSON数组)',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 考勤组表
CREATE TABLE oa_attendance_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL COMMENT '考勤组名称',
    shift_id BIGINT COMMENT '班次ID',
    applicable_depts VARCHAR(500) COMMENT '适用部门(JSON数组)',
    applicable_users VARCHAR(500) COMMENT '适用用户(JSON数组)',
    check_location VARCHAR(500) COMMENT '考勤地点(JSON数组)',
    check_range INT DEFAULT 500 COMMENT '考勤范围(米)',
    allow_remote_check TINYINT DEFAULT 0 COMMENT '允许远程打卡',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 节假日配置表
CREATE TABLE oa_attendance_holiday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    holiday_date DATE NOT NULL COMMENT '节假日日期',
    holiday_name VARCHAR(100) NOT NULL COMMENT '节假日名称',
    holiday_type VARCHAR(20) DEFAULT 'HOLIDAY' COMMENT '类型:HOLIDAY节假日,WORKDAY调休上班',
    year INT NOT NULL COMMENT '年份',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 1.2 考勤规则JSON配置示例

```json
// 加班规则配置
{
  "overtimeType": ["WORKDAY", "REST_DAY", "HOLIDAY"],
  "approvalRequired": true,
  "minApplyHours": 0.5,
  "calculationByApproval": true,
  "breakRules": {
    "WORKDAY": 1.0,
    "REST_DAY": 1.0,
    "HOLIDAY": 1.0
  },
  "validityMonths": 6
}

// 补卡规则配置
{
  "maxRepairCount": 5,
  "countResetDay": 1,
  "repairRangeWorkDays": 31,
  "repairType": ["MISSING", "LATE", "EARLY_LEAVE"],
  "requireReason": true,
  "requireAttachment": false,
  "approvalRequired": true
}
```

> **说明**：以上规则均可由管理员在后台配置修改。

---

## 2. 加班模块设计

### 2.1 数据模型

```sql
-- 加班申请表
CREATE TABLE oa_overtime (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    overtime_no VARCHAR(32) NOT NULL UNIQUE COMMENT '加班单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    overtime_type VARCHAR(20) NOT NULL COMMENT '加班类型:WORKDAY,REST_DAY,HOLIDAY',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    hours DECIMAL(5,1) NOT NULL COMMENT '加班时长(小时)',
    reason TEXT COMMENT '加班原因',
    compensation_type VARCHAR(20) DEFAULT 'BREAK' COMMENT '补偿方式:BREAK调休,SALARY加班费',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING,APPROVED,REJECTED,COMPLETED',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    approved_hours DECIMAL(5,1) COMMENT '审批通过的时长',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_overtime_type (overtime_type),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 加班调休记录表
CREATE TABLE oa_overtime_break (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    overtime_id BIGINT NOT NULL COMMENT '加班记录ID',
    available_hours DECIMAL(5,1) NOT NULL COMMENT '可用调休时长',
    used_hours DECIMAL(5,1) DEFAULT 0 COMMENT '已使用时长',
    expired_time DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_expired_time (expired_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.2 加班类型枚举

```java
/**
 * 加班类型枚举
 * 说明：加班时长以审批单填写时间为准
 */
public enum OvertimeType {
    WORKDAY("WORKDAY", "工作日加班"),
    REST_DAY("REST_DAY", "休息日加班"),
    HOLIDAY("HOLIDAY", "节假日加班");

    private final String code;
    private final String desc;

    OvertimeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}

/**
 * 加班补偿类型
 */
public enum CompensationType {
    BREAK("BREAK", "调休"),
    SALARY("SALARY", "加班费");

    private final String code;
    private final String desc;

    CompensationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
```

### 2.3 加班时长计算规则

```java
/**
 * 加班时长计算服务
 * 说明：加班时长以审批单填写的开始/结束时间为准
 */
@Service
@Slf4j
public class OvertimeCalculationService {

    /**
     * 计算加班时长
     * 规则：加班时长以审批单填写的 start_time 和 end_time 为准
     * 最小单位：0.5小时
     */
    public BigDecimal calculateOvertimeHours(LocalDateTime startTime,
                                              LocalDateTime endTime) {
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();

        // 转换为小时，保留0.5小时精度
        BigDecimal hours = BigDecimal.valueOf(totalMinutes)
            .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);

        // 最小加班单位：0.5小时
        hours = hours.setScale(1, RoundingMode.HALF_UP);
        return hours;
    }

    /**
     * 计算调休时长
     * 说明：当前统一为1:1，管理员可配置不同比例
     */
    public BigDecimal calculateBreakHours(OvertimeType type, BigDecimal overtimeHours) {
        // 当前统一1:1，可扩展为按配置计算
        return overtimeHours;
    }

    /**
     * 计算加班费
     * 时薪 = 月薪 / 21.75 / 8
     */
    public BigDecimal calculateOvertimeSalary(BigDecimal baseSalary,
                                              BigDecimal overtimeHours) {
        BigDecimal hourlyRate = baseSalary.divide(
            BigDecimal.valueOf(21.75 * 8), 2, RoundingMode.HALF_UP);
        return hourlyRate.multiply(overtimeHours);
    }
}
```

### 2.4 加班API接口

```java
/**
 * 加班申请Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/overtime")
@Tag(name = "加班申请", description = "加班申请相关接口")
public class OvertimeController {

    @PostMapping
    @Operation(summary = "新建加班申请")
    public Result<Long> createOvertime(@Valid @RequestBody OvertimeForm form,
                                        @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(overtimeService.createOvertime(form, userId));
    }

    @GetMapping
    @Operation(summary = "加班列表")
    public Result<PageResult<OvertimeVO>> listOvertime(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String overtimeType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(overtimeService.listOvertime(pageNum, pageSize, status, overtimeType, startDate, endDate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "加班详情")
    public Result<OvertimeVO> getOvertimeById(@PathVariable Long id) {
        return Result.success(overtimeService.getOvertimeById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批加班")
    public Result<Void> approveOvertime(@PathVariable Long id,
                                        @Valid @RequestBody ApproveForm form,
                                        @RequestHeader("Authorization") String token) {
        Long approverId = getUserIdFromToken(token);
        overtimeService.approveOvertime(id, form, approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "撤回加班申请")
    public Result<Void> cancelOvertime(@PathVariable Long id,
                                        @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        overtimeService.cancelOvertime(id, userId);
        return Result.success();
    }

    @GetMapping("/balance")
    @Operation(summary = "查询调休余额")
    public Result<OvertimeBalanceVO> getOvertimeBalance(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(overtimeService.getOvertimeBalance(userId));
    }
}
```

---

## 3. 补卡模块设计

### 3.1 数据模型

```sql
-- 补卡申请表
CREATE TABLE oa_repair_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repair_no VARCHAR(32) NOT NULL UNIQUE COMMENT '补卡单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    repair_date DATE NOT NULL COMMENT '补卡日期',
    repair_type VARCHAR(20) NOT NULL COMMENT '补卡类型:MISSING缺卡,LATE迟到,EARLY_LEAVE早退',
    repair_time DATETIME NOT NULL COMMENT '补卡时间',
    original_time DATETIME COMMENT '原本应该打卡的时间',
    reason VARCHAR(200) NOT NULL COMMENT '补卡原因',
    attachments VARCHAR(500) COMMENT '证明材料URL',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING待审批,APPROVED已通过,REJECTED已拒绝',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    approver_comment TEXT COMMENT '审批意见',
    approved_time DATETIME COMMENT '审批时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_repair_date (repair_date),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 补卡次数统计表（按月统计，每月从1日起算）
CREATE TABLE oa_repair_card_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    year_month VARCHAR(7) NOT NULL COMMENT '统计月份',
    repair_count INT DEFAULT 0 COMMENT '补卡次数',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month (user_id, year_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.1.1 补卡类型枚举

```java
/**
 * 补卡类型枚举
 */
public enum RepairType {
    MISSING("MISSING", "缺卡"),
    LATE("LATE", "迟到"),
    EARLY_LEAVE("EARLY_LEAVE", "早退");

    private final String code;
    private final String desc;

    RepairType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
```

### 3.2 补卡规则验证

```java
/**
 * 补卡规则验证服务
 */
@Service
@Slf4j
public class RepairCardRuleService {

    @Autowired
    private RepairCardMapper repairCardMapper;

    @Autowired
    private AttendanceRuleConfig ruleConfig;

    /**
     * 验证补卡申请
     * 规则：
     * 1. 每月可提交5次（计时周期从每月1日起算）
     * 2. 可申请最近31个工作日内的补卡
     * 3. 补卡时间不能晚于当前时间
     * 4. 补卡类型：缺卡(MISSING)、迟到(LATE)、早退(EARLY_LEAVE)
     */
    public void validateRepairCard(RepairCardForm form, Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate repairDate = form.getRepairDate();

        // 规则1：检查补卡次数（每月5次）
        if (!checkRepairCount(userId, now)) {
            throw new BusinessException("本月补卡次数已达上限(5次)");
        }

        // 规则2：检查补卡时间范围（31个工作日内）
        int workDays = calculateWorkDays(repairDate, now);
        if (workDays > 31) {
            throw new BusinessException("仅可申请最近31个工作日内的补卡");
        }

        // 规则3：不能补未来的卡
        if (repairDate.isAfter(now)) {
            throw new BusinessException("不能补未来的打卡记录");
        }

        // 规则4：补卡时间有效性
        LocalDateTime repairTime = form.getRepairTime();
        if (repairTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException("补卡时间不能晚于当前时间");
        }
    }

    /**
     * 计算两个日期之间的工作日天数
     */
    private int calculateWorkDays(LocalDate startDate, LocalDate endDate) {
        int workDays = 0;
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workDays++;
            }
            date = date.plusDays(1);
        }
        return workDays;
    }

    /**
     * 检查补卡次数（每月从1日起算）
     */
    private boolean checkRepairCount(Long userId, LocalDate now) {
        // 计算当月1日到今天的补卡次数
        LocalDate monthStart = now.withDayOfMonth(1);
        String startDate = monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int count = repairCardMapper.countByDateRange(userId, startDate, endDate);
        return count < ruleConfig.getMaxRepairCount();
    }
}
```

### 3.3 补卡API接口

```java
/**
 * 补卡申请Controller
 */
@RestController
@RequestMapping("/api/v1/attendance/repair")
@Tag(name = "补卡申请", description = "补卡申请相关接口")
public class RepairCardController {

    @PostMapping
    @Operation(summary = "新建补卡申请")
    public Result<Long> createRepairCard(@Valid @RequestBody RepairCardForm form,
                                          @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        repairCardRuleService.validateRepairCard(form, userId);
        return Result.success(repairCardService.createRepairCard(form, userId));
    }

    @GetMapping
    @Operation(summary = "补卡列表")
    public Result<PageResult<RepairCardVO>> listRepairCard(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(repairCardService.listRepairCard(pageNum, pageSize, status, startDate, endDate, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "补卡详情")
    public Result<RepairCardVO> getRepairCardById(@PathVariable Long id) {
        return Result.success(repairCardService.getRepairCardById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批补卡")
    public Result<Void> approveRepairCard(@PathVariable Long id,
                                          @Valid @RequestBody ApproveForm form,
                                          @RequestHeader("Authorization") String token) {
        Long approverId = getUserIdFromToken(token);
        repairCardService.approveRepairCard(id, form, approverId);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "补卡统计")
    public Result<RepairCardStatisticsVO> getStatistics(
            @RequestParam(required = false) String yearMonth,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(repairCardService.getStatistics(userId, yearMonth));
    }
}
```

---

## 4. 出差模块设计

### 4.1 数据模型

```sql
-- 出差申请表
CREATE TABLE oa_business_trip (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trip_no VARCHAR(32) NOT NULL UNIQUE COMMENT '出差单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    destination VARCHAR(200) NOT NULL COMMENT '出差目的地',
    trip_type VARCHAR(20) NOT NULL COMMENT '出差类型:LOCAL本地,SHORT短途,LONG长途',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    days DECIMAL(5,1) NOT NULL COMMENT '出差天数',
    reason TEXT NOT NULL COMMENT '出差事由',
    budget_amount DECIMAL(10,2) COMMENT '预算金额',
    budget_remark VARCHAR(200) COMMENT '预算说明',
    attachments VARCHAR(500) COMMENT '附件',
    emergency_contact VARCHAR(50) COMMENT '紧急联系人',
    emergency_phone VARCHAR(20) COMMENT '紧急联系电话',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_start_date (start_date),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 出差补贴记录表
CREATE TABLE oa_business_trip_subsidy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trip_id BIGINT NOT NULL COMMENT '出差记录ID',
    subsidy_type VARCHAR(20) NOT NULL COMMENT '补贴类型:TRANSPORT,ACCOMMODATION,DAILY',
    amount DECIMAL(10,2) NOT NULL COMMENT '补贴金额',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trip_id (trip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.2 出差类型枚举

```java
/**
 * 出差类型枚举
 */
public enum BusinessTripType {
    LOCAL("LOCAL", "本地出差", 50),
    SHORT("SHORT", "短途出差", 150),
    LONG("LONG", "长途出差", 200);

    private final String code;
    private final String desc;
    private final BigDecimal dailySubsidy;

    BusinessTripType(String code, String desc, BigDecimal dailySubsidy) {
        this.code = code;
        this.desc = desc;
        this.dailySubsidy = dailySubsidy;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
    public BigDecimal getDailySubsidy() { return dailySubsidy; }

    public static BusinessTripType calculateType(int days) {
        if (days <= 1) return LOCAL;
        if (days <= 3) return SHORT;
        return LONG;
    }
}
```

### 4.3 出差API接口

```java
/**
 * 出差申请Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/business-trip")
@Tag(name = "出差申请", description = "出差申请相关接口")
public class BusinessTripController {

    @PostMapping
    @Operation(summary = "新建出差申请")
    public Result<Long> createBusinessTrip(@Valid @RequestBody BusinessTripForm form,
                                            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(businessTripService.createBusinessTrip(form, userId));
    }

    @GetMapping
    @Operation(summary = "出差列表")
    public Result<PageResult<BusinessTripVO>> listBusinessTrip(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(businessTripService.listBusinessTrip(pageNum, pageSize, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "出差详情")
    public Result<BusinessTripVO> getBusinessTripById(@PathVariable Long id) {
        return Result.success(businessTripService.getBusinessTripById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批出差")
    public Result<Void> approveBusinessTrip(@PathVariable Long id,
                                             @Valid @RequestBody ApproveForm form,
                                             @RequestHeader("Authorization") String token) {
        Long approverId = getUserIdFromToken(token);
        businessTripService.approveBusinessTrip(id, form, approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "撤回出差申请")
    public Result<Void> cancelBusinessTrip(@PathVariable Long id,
                                            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        businessTripService.cancelBusinessTrip(id, userId);
        return Result.success();
    }
}
```

---

## 5. 假期管理设计

### 5.1 数据模型

```sql
-- 假期类型表
CREATE TABLE oa_leave_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    leave_code VARCHAR(20) NOT NULL UNIQUE COMMENT '假期编码',
    leave_name VARCHAR(50) NOT NULL COMMENT '假期名称',
    default_days DECIMAL(5,1) DEFAULT 0 COMMENT '默认天数',
    max_days DECIMAL(5,1) DEFAULT 0 COMMENT '最大天数(0=无限制)',
    requires_proof TINYINT DEFAULT 0 COMMENT '是否需要证明材料',
    can_transfer TINYINT DEFAULT 0 COMMENT '是否可结转',
    transfer_max_days DECIMAL(5,1) DEFAULT 0 COMMENT '可结转最大天数',
    valid_months INT DEFAULT 0 COMMENT '有效期(月),0=永久有效',
    deduct_salary TINYINT DEFAULT 0 COMMENT '是否扣工资',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 员工假期余额表
CREATE TABLE oa_leave_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    leave_type VARCHAR(20) NOT NULL COMMENT '假期类型',
    year INT NOT NULL COMMENT '年度',
    total_days DECIMAL(5,1) DEFAULT 0 COMMENT '可用天数',
    used_days DECIMAL(5,1) DEFAULT 0 COMMENT '已使用天数',
    pending_days DECIMAL(5,1) DEFAULT 0 COMMENT '待审批天数',
    transferred_days DECIMAL(5,1) DEFAULT 0 COMMENT '已结转天数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_type_year (user_id, leave_type, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 请假申请表(扩展)
CREATE TABLE oa_leave (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    leave_no VARCHAR(32) NOT NULL UNIQUE COMMENT '请假单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    dept_id BIGINT COMMENT '部门ID',
    leave_type VARCHAR(20) NOT NULL COMMENT '请假类型',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    start_time TIME COMMENT '开始时间(半天假)',
    end_time TIME COMMENT '结束时间(半天假)',
    days DECIMAL(5,1) NOT NULL COMMENT '请假天数',
    hours DECIMAL(5,1) DEFAULT 0 COMMENT '请假小时数(半天假)',
    reason TEXT COMMENT '请假事由',
    attachments VARCHAR(500) COMMENT '附件',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    process_instance_id VARCHAR(100) COMMENT 'Camunda流程实例ID',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_leave_type (leave_type),
    INDEX idx_status (status),
    INDEX idx_start_date (start_date),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.2 假期类型枚举

```java
/**
 * 假期类型枚举
 */
public enum LeaveType {
    ANNUAL("ANNUAL", "年假", true, true, 0, false),
    SICK("SICK", "病假", false, false, 0, false),
    PERSONAL("PERSONAL", "事假", false, false, 10, true),
    MARRIAGE("MARRIAGE", "婚假", true, false, 0, false),
    MATERNITY("MATERNITY", "产假", true, false, 0, false),
    PATERNITY("PATERNITY", "陪产假", true, false, 0, false),
    FUNERAL("FUNERAL", "丧假", true, false, 0, false),
    WORK_INJURY("WORK_INJURY", "工伤假", true, false, 0, false);

    private final String code;
    private final String desc;
    private final boolean hasQuota;
    private final boolean canTransfer;
    private final int defaultDays;
    private final boolean deductSalary;

    LeaveType(String code, String desc, boolean hasQuota, boolean canTransfer,
              int defaultDays, boolean deductSalary) {
        this.code = code;
        this.desc = desc;
        this.hasQuota = hasQuota;
        this.canTransfer = canTransfer;
        this.defaultDays = defaultDays;
        this.deductSalary = deductSalary;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
    public boolean hasQuota() { return hasQuota; }
    public boolean canTransfer() { return canTransfer; }
    public int getDefaultDays() { return defaultDays; }
    public boolean deductSalary() { return deductSalary; }
}

/**
 * 年假计算规则
 */
public class AnnualLeaveCalculator {

    /**
     * 根据工龄计算年假天数
     */
    public static int calculateAnnualLeave(int workYears) {
        if (workYears < 1) return 0;
        if (workYears < 5) return 5;
        if (workYears < 10) return 10;
        if (workYears < 20) return 15;
        return 20;
    }

    /**
     * 计算剩余年假（年底结转）
     */
    public static BigDecimal calculateRemainingLeave(BigDecimal totalDays,
                                                       BigDecimal usedDays,
                                                       LocalDate endDate,
                                                       int maxTransferDays) {
        BigDecimal remaining = totalDays.subtract(usedDays);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 距离年底剩余月份数
        int remainingMonths = endDate.until(LocalDate.of(endDate.getYear(), 12, 31)).getMonths();

        // 可结转天数 = min(剩余天数, 最大结转天数, 按月份折算)
        int monthlyDeduct = remainingDays.intValue() / (remainingMonths + 1);
        int transferDays = Math.min(remaining.intValue(),
            Math.min(maxTransferDays, monthlyDeduct));

        return BigDecimal.valueOf(transferDays);
    }
}
```

### 5.3 假期API接口

```java
/**
 * 假期管理Controller
 */
@RestController
@RequestMapping("/api/v1/attendance/leave")
@Tag(name = "假期管理", description = "假期管理相关接口")
public class LeaveBalanceController {

    @GetMapping("/types")
    @Operation(summary = "获取假期类型列表")
    public Result<List<LeaveTypeVO>> getLeaveTypes() {
        return Result.success(leaveBalanceService.getLeaveTypes());
    }

    @GetMapping("/balance")
    @Operation(summary = "获取我的假期余额")
    public Result<List<LeaveBalanceVO>> getMyLeaveBalance(
            @RequestParam(required = false) Integer year,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(leaveBalanceService.getLeaveBalance(userId, year));
    }

    @GetMapping("/balance/{userId}")
    @Operation(summary = "获取员工假期余额(管理员)")
    public Result<List<LeaveBalanceVO>> getUserLeaveBalance(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year) {
        return Result.success(leaveBalanceService.getLeaveBalance(userId, year));
    }

    @PostMapping("/balance/init")
    @Operation(summary = "初始化员工假期余额(管理员)")
    public Result<Void> initLeaveBalance(@RequestBody InitLeaveBalanceForm form,
                                         @RequestHeader("Authorization") String token) {
        Long adminId = getUserIdFromToken(token);
        leaveBalanceService.initLeaveBalance(form, adminId);
        return Result.success();
    }

    @PostMapping("/balance/adjust")
    @Operation(summary = "调整员工假期余额(管理员)")
    public Result<Void> adjustLeaveBalance(@RequestBody AdjustLeaveBalanceForm form,
                                           @RequestHeader("Authorization") String token) {
        Long adminId = getUserIdFromToken(token);
        leaveBalanceService.adjustLeaveBalance(form, adminId);
        return Result.success();
    }

    @PostMapping
    @Operation(summary = "新建请假申请")
    public Result<Long> createLeave(@Valid @RequestBody LeaveForm form,
                                    @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        leaveService.validateAndDeductBalance(form, userId);
        return Result.success(leaveService.createLeave(form, userId));
    }

    @GetMapping
    @Operation(summary = "请假列表")
    public Result<PageResult<LeaveVO>> listLeave(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(leaveService.listLeave(pageNum, pageSize, status, leaveType, startDate, endDate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "请假详情")
    public Result<LeaveVO> getLeaveById(@PathVariable Long id) {
        return Result.success(leaveService.getLeaveById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批请假")
    public Result<Void> approveLeave(@PathVariable Long id,
                                       @Valid @RequestBody ApproveForm form,
                                       @RequestHeader("Authorization") String token) {
        Long approverId = getUserIdFromToken(token);
        leaveService.approveLeave(id, form, approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "撤回请假申请")
    public Result<Void> cancelLeave(@PathVariable Long id,
                                     @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        leaveService.cancelLeaveAndReturnBalance(id, userId);
        return Result.success();
    }
}
```

---

## 6. 工资管理设计

### 6.1 数据模型

```sql
-- 工资单主表
CREATE TABLE oa_salary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_no VARCHAR(32) NOT NULL UNIQUE COMMENT '工资单号',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    dept_id BIGINT COMMENT '部门ID',
    year_month VARCHAR(7) NOT NULL COMMENT '工资月份',
    base_salary DECIMAL(10,2) NOT NULL COMMENT '基本工资',
    position_salary DECIMAL(10,2) DEFAULT 0 COMMENT '岗位工资',
    performance_salary DECIMAL(10,2) DEFAULT 0 COMMENT '绩效工资',
    overtime_salary DECIMAL(10,2) DEFAULT 0 COMMENT '加班费',
    bonus DECIMAL(10,2) DEFAULT 0 COMMENT '奖金',
    subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '补贴',
    other_addition DECIMAL(10,2) DEFAULT 0 COMMENT '其他加项',
    social_insurance DECIMAL(10,2) DEFAULT 0 COMMENT '社保公积金',
    personal_tax DECIMAL(10,2) DEFAULT 0 COMMENT '个人所得税',
    late_fine DECIMAL(10,2) DEFAULT 0 COMMENT '迟到扣款',
    other_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '其他扣款',
    gross_salary DECIMAL(10,2) DEFAULT 0 COMMENT '应发工资',
    net_salary DECIMAL(10,2) DEFAULT 0 COMMENT '实发工资',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT草稿,PENDING审批中,APPROVED已审批,PAID已发放',
    creator_id BIGINT COMMENT '制单人',
    approver_id BIGINT COMMENT '审批人',
    approve_time DATETIME COMMENT '审批时间',
    approve_comment TEXT COMMENT '审批意见',
    paid_time DATETIME COMMENT '发放时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month (user_id, year_month),
    INDEX idx_year_month (year_month),
    INDEX idx_status (status),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 工资项配置表
CREATE TABLE oa_salary_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_code VARCHAR(50) NOT NULL UNIQUE COMMENT '工资项编码',
    item_name VARCHAR(100) NOT NULL COMMENT '工资项名称',
    item_type VARCHAR(20) NOT NULL COMMENT '类型:ADDITION加项,DEDUCTION扣项',
    category VARCHAR(20) COMMENT '类别:BASE基础,PERFORMANCE绩效,ALLOWANCE补贴,FINE扣款',
    default_value DECIMAL(10,2) DEFAULT 0 COMMENT '默认值',
    is_taxable TINYINT DEFAULT 1 COMMENT '是否计税',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 工资条发送记录表
CREATE TABLE oa_salary_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_id BIGINT NOT NULL COMMENT '工资单ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    send_way VARCHAR(20) DEFAULT 'APP' COMMENT '发送方式:APP,EMAIL,PAPER',
    send_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '发送状态:PENDING,SUCCESS,FAILED',
    send_time DATETIME COMMENT '发送时间',
    read_time DATETIME COMMENT '查看时间',
    fail_reason VARCHAR(200) COMMENT '失败原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_salary_id (salary_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 工资审批记录表
CREATE TABLE oa_salary_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_id BIGINT NOT NULL COMMENT '工资单ID',
    year_month VARCHAR(7) NOT NULL COMMENT '工资月份',
    total_count INT DEFAULT 0 COMMENT '总人数',
    total_amount DECIMAL(14,2) DEFAULT 0 COMMENT '总金额',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    creator_id BIGINT COMMENT '制单人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_year_month (year_month),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.2 工资计算规则

```java
/**
 * 工资计算服务
 */
@Service
@Slf4j
public class SalaryCalculationService {

    /**
     * 计算工资
     */
    public Salary calculateSalary(SalaryForm form, UserDTO user) {
        Salary salary = new Salary();
        salary.setUserId(user.getId());
        salary.setDeptId(user.getDeptId());
        salary.setYearMonth(form.getYearMonth());

        // 应发工资计算
        BigDecimal grossSalary = BigDecimal.ZERO;
        grossSalary = grossSalary.add(form.getBaseSalary());           // 基本工资
        grossSalary = grossSalary.add(form.getPositionSalary());       // 岗位工资
        grossSalary = grossSalary.add(form.getPerformanceSalary());    // 绩效工资
        grossSalary = grossSalary.add(form.getOvertimeSalary());        // 加班费
        grossSalary = grossSalary.add(form.getBonus());                // 奖金
        grossSalary = grossSalary.add(form.getSubsidy());              // 补贴
        grossSalary = grossSalary.add(form.getOtherAddition());         // 其他加项
        salary.setGrossSalary(grossSalary);

        // 扣款计算
        BigDecimal deductions = BigDecimal.ZERO;
        deductions = deductions.add(form.getSocialInsurance());        // 社保公积金
        deductions = deductions.add(form.getPersonalTax());           // 个税
        deductions = deductions.add(form.getLateFine());              // 迟到扣款
        deductions = deductions.add(form.getOtherDeduction());        // 其他扣款

        // 计算个税（累计预扣法）
        BigDecimal tax = calculatePersonalTax(grossSalary, form.getDeductions());
        deductions = deductions.add(tax);
        salary.setPersonalTax(tax);

        // 实发工资
        salary.setNetSalary(grossSalary.subtract(deductions));
        salary.setSocialInsurance(form.getSocialInsurance());
        salary.setLateFine(form.getLateFine());
        salary.setOtherDeduction(form.getOtherDeduction());

        return salary;
    }

    /**
     * 计算个人所得税（累计预扣法简化版）
     */
    private BigDecimal calculatePersonalTax(BigDecimal taxableIncome,
                                            BigDecimal deductions) {
        // 免税额：5000元/月
        BigDecimal taxFree = BigDecimal.valueOf(5000);
        BigDecimal taxable = taxableIncome.subtract(deductions).subtract(taxFree);

        if (taxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 税率表（简化版）
        if (taxable.compareTo(BigDecimal.valueOf(3000)) <= 0) {
            return taxable.multiply(BigDecimal.valueOf(0.03));
        } else if (taxable.compareTo(BigDecimal.valueOf(12000)) <= 0) {
            return taxable.multiply(BigDecimal.valueOf(0.10)).subtract(BigDecimal.valueOf(210));
        } else if (taxable.compareTo(BigDecimal.valueOf(25000)) <= 0) {
            return taxable.multiply(BigDecimal.valueOf(0.20)).subtract(BigDecimal.valueOf(1410));
        } else if (taxable.compareTo(BigDecimal.valueOf(35000)) <= 0) {
            return taxable.multiply(BigDecimal.valueOf(0.25)).subtract(BigDecimal.valueOf(2660));
        } else if (taxable.compareTo(BigDecimal.valueOf(55000)) <= 0) {
            return taxable.multiply(BigDecimal.valueOf(0.30)).subtract(BigDecimal.valueOf(4410));
        } else if (taxable.compareTo(BigDecimal.valueOf(80000)) <= 0) {
            return taxable.multiply(BigDecimal.valueOf(0.35)).subtract(BigDecimal.valueOf(7160));
        } else {
            return taxable.multiply(BigDecimal.valueOf(0.45)).subtract(BigDecimal.valueOf(15160));
        }
    }

    /**
     * 批量计算工资
     */
    public void batchCalculateSalary(String yearMonth, List<Long> userIds) {
        for (Long userId : userIds) {
            try {
                UserDTO user = getUserById(userId);
                SalaryForm form = new SalaryForm();
                form.setYearMonth(yearMonth);
                form.setBaseSalary(user.getBaseSalary());
                // ... 其他字段

                Salary salary = calculateSalary(form, user);
                salaryMapper.insert(salary);
            } catch (Exception e) {
                log.error("计算工资失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }
}
```

### 6.3 工资API接口

```java
/**
 * 工资管理Controller
 */
@RestController
@RequestMapping("/api/v1/finance/salary")
@Tag(name = "工资管理", description = "工资管理相关接口")
public class SalaryController {

    @GetMapping("/items")
    @Operation(summary = "获取工资项配置")
    public Result<List<SalaryItemVO>> getSalaryItems() {
        return Result.success(salaryService.getSalaryItems());
    }

    @PostMapping
    @Operation(summary = "制作工资单")
    public Result<Long> createSalary(@Valid @RequestBody SalaryForm form,
                                    @RequestHeader("Authorization") String token) {
        Long creatorId = getUserIdFromToken(token);
        return Result.success(salaryService.createSalary(form, creatorId));
    }

    @PostMapping("/batch")
    @Operation(summary = "批量制作工资单")
    public Result<Integer> batchCreateSalary(@RequestBody BatchSalaryForm form,
                                            @RequestHeader("Authorization") String token) {
        Long creatorId = getUserIdFromToken(token);
        return Result.success(salaryService.batchCreateSalary(form, creatorId));
    }

    @GetMapping
    @Operation(summary = "工资单列表")
    public Result<PageResult<SalaryVO>> listSalary(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deptId) {
        return Result.success(salaryService.listSalary(pageNum, pageSize, yearMonth, status, deptId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "工资单详情")
    public Result<SalaryVO> getSalaryById(@PathVariable Long id) {
        return Result.success(salaryService.getSalaryById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改工资单")
    public Result<Void> updateSalary(@PathVariable Long id,
                                      @Valid @RequestBody SalaryForm form,
                                      @RequestHeader("Authorization") String token) {
        Long updaterId = getUserIdFromToken(token);
        salaryService.updateSalary(id, form, updaterId);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交工资审批")
    public Result<Void> submitSalary(@PathVariable Long id,
                                     @RequestHeader("Authorization") String token) {
        Long submitterId = getUserIdFromToken(token);
        salaryService.submitSalary(id, submitterId);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批工资单")
    public Result<Void> approveSalary(@PathVariable Long id,
                                      @Valid @RequestBody ApproveForm form,
                                      @RequestHeader("Authorization") String token) {
        Long approverId = getUserIdFromToken(token);
        salaryService.approveSalary(id, form, approverId);
        return Result.success();
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "发放工资")
    public Result<Void> paySalary(@PathVariable Long id,
                                   @RequestHeader("Authorization") String token) {
        Long operatorId = getUserIdFromToken(token);
        salaryService.paySalary(id, operatorId);
        return Result.success();
    }

    @PostMapping("/batch-pay")
    @Operation(summary = "批量发放工资")
    public Result<Integer> batchPaySalary(@RequestBody BatchPayForm form,
                                           @RequestHeader("Authorization") String token) {
        Long operatorId = getUserIdFromToken(token);
        return Result.success(salaryService.batchPaySalary(form, operatorId));
    }

    @GetMapping("/my")
    @Operation(summary = "我的工资条")
    public Result<List<SalaryVO>> getMySalary(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(salaryService.getMySalary(userId, startMonth, endMonth));
    }

    @GetMapping("/export")
    @Operation(summary = "导出工资表")
    public void exportSalary(@RequestParam String yearMonth,
                             @RequestParam(required = false) Long deptId,
                             HttpServletResponse response) {
        salaryService.exportSalary(yearMonth, deptId, response);
    }
}
```

---

## 7. 菜单结构设计

### 7.1 菜单JSON配置

```json
{
  "menuConfig": [
    {
      "code": "HOME",
      "name": "首页",
      "icon": "HomeFilled",
      "path": "/home",
      "children": [
        {"code": "WORKBENCH", "name": "工作台", "path": "/home/workbench"},
        {"code": "DASHBOARD", "name": "数据看板", "path": "/home/dashboard"}
      ]
    },
    {
      "code": "OA_APPROVAL",
      "name": "OA审批",
      "icon": "DocumentChecked",
      "path": "/approval",
      "children": [
        {"code": "TRAVEL", "name": "差旅报销", "path": "/approval/travel"},
        {"code": "LEAVE", "name": "请假", "path": "/approval/leave"},
        {"code": "PURCHASE", "name": "采购", "path": "/approval/purchase"},
        {"code": "SALARY", "name": "工资", "path": "/approval/salary"},
        {"code": "STAMP", "name": "用章", "path": "/approval/stamp"},
        {"code": "OVERTIME", "name": "加班", "path": "/approval/overtime"},
        {"code": "BUSINESS_TRIP", "name": "出差", "path": "/approval/business-trip"},
        {"code": "REPAIR_CARD", "name": "补卡", "path": "/approval/repair-card"},
        {"code": "OUTING", "name": "外出", "path": "/approval/outing"}
      ]
    },
    {
      "code": "ATTENDANCE",
      "name": "考勤",
      "icon": "Clock",
      "path": "/attendance",
      "children": [
        {"code": "CHECK", "name": "打卡", "path": "/attendance/check"},
        {"code": "STATISTICS", "name": "考勤统计", "path": "/attendance/statistics"},
        {"code": "EXCEPTION", "name": "异常处理", "path": "/attendance/exception"}
      ]
    },
    {
      "code": "APPROVAL_RECORD",
      "name": "审批记录",
      "icon": "Document",
      "path": "/approval-record",
      "children": [
        {"code": "MY_APPLY", "name": "我发起的", "path": "/approval-record/my-apply"},
        {"code": "MY_APPROVED", "name": "我审批的", "path": "/approval-record/my-approved"},
        {"code": "ALL_RECORD", "name": "全部记录", "path": "/approval-record/all"}
      ]
    },
    {
      "code": "MESSAGE",
      "name": "消息",
      "icon": "Message",
      "path": "/message",
      "children": [
        {"code": "SYSTEM_NOTICE", "name": "系统通知", "path": "/message/system"},
        {"code": "MY_MESSAGE", "name": "我的消息", "path": "/message/my"}
      ]
    },
    {
      "code": "PERSONAL",
      "name": "个人中心",
      "icon": "User",
      "path": "/personal",
      "children": [
        {"code": "SALARY_SLIP", "name": "工资条", "path": "/personal/salary-slip"},
        {"code": "LEAVE_RECORD", "name": "请假记录", "path": "/personal/leave-record"},
        {"code": "ATTENDANCE_RECORD", "name": "考勤记录", "path": "/personal/attendance-record"},
        {"code": "MY_INFO", "name": "个人信息", "path": "/personal/my-info"}
      ]
    },
    {
      "code": "SYSTEM",
      "name": "系统管理",
      "icon": "Setting",
      "path": "/system",
      "permission": "ADMIN",
      "children": [
        {"code": "USER_MANAGE", "name": "用户管理", "path": "/system/user"},
        {"code": "DEPT_MANAGE", "name": "部门管理", "path": "/system/dept"},
        {"code": "ROLE_PERMISSION", "name": "角色权限", "path": "/system/role"},
        {"code": "ATTENDANCE_RULE", "name": "考勤规则", "path": "/system/attendance-rule"},
        {"code": "APPROVAL_FLOW", "name": "审批流程", "path": "/system/approval-flow"},
        {"code": "SYSTEM_CONFIG", "name": "系统配置", "path": "/system/config"}
      ]
    }
  ]
}
```

### 7.2 前端路由配置

```javascript
// router/index.js
export const routes = [
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页' },
    children: [
      { path: 'workbench', name: 'Workbench', component: () => import('@/views/home/workbench.vue') },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/home/dashboard.vue') }
    ]
  },
  {
    path: '/approval',
    name: 'OAApproval',
    component: () => import('@/views/approval/index.vue'),
    meta: { title: 'OA审批' },
    children: [
      { path: 'travel', name: 'Travel', component: () => import('@/views/approval/travel.vue') },
      { path: 'leave', name: 'Leave', component: () => import('@/views/approval/leave.vue') },
      { path: 'purchase', name: 'Purchase', component: () => import('@/views/approval/purchase.vue') },
      { path: 'salary', name: 'Salary', component: () => import('@/views/approval/salary.vue') },
      { path: 'stamp', name: 'Stamp', component: () => import('@/views/approval/stamp.vue') },
      { path: 'overtime', name: 'Overtime', component: () => import('@/views/approval/overtime.vue') },
      { path: 'business-trip', name: 'BusinessTrip', component: () => import('@/views/approval/business-trip.vue') },
      { path: 'repair-card', name: 'RepairCard', component: () => import('@/views/approval/repair-card.vue') },
      { path: 'outing', name: 'Outing', component: () => import('@/views/approval/outing.vue') }
    ]
  },
  // ... 其他路由
]
```

---

## 8. 审批记录功能设计

### 8.1 数据模型

```sql
-- 审批记录表(统一)
CREATE TABLE oa_approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_no VARCHAR(32) NOT NULL UNIQUE COMMENT '记录编号',
    business_type VARCHAR(20) NOT NULL COMMENT '业务类型:LEAVE,EXPENSE,PURCHASE,SALARY,STAMP,OVERTIME,BUSINESS_TRIP,REPAIR_CARD,OUTING',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    user_name VARCHAR(50) COMMENT '申请人姓名',
    dept_id BIGINT COMMENT '部门ID',
    dept_name VARCHAR(100) COMMENT '部门名称',
    title VARCHAR(200) NOT NULL COMMENT '标题/摘要',
    content JSON COMMENT '申请内容摘要(JSON)',
    amount DECIMAL(14,2) COMMENT '金额(如有)',
    start_date DATE COMMENT '开始日期(如有)',
    end_date DATE COMMENT '结束日期(如有)',
    status VARCHAR(20) NOT NULL COMMENT '状态:PENDING审批中,APPROVED已通过,REJECTED已拒绝,CANCELLED已撤回',
    current_node VARCHAR(50) COMMENT '当前节点',
    total_nodes INT DEFAULT 1 COMMENT '总节点数',
    completed_nodes INT DEFAULT 0 COMMENT '已完成节点数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_business_type (business_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 审批节点明细表
CREATE TABLE oa_approval_node_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT '记录ID',
    node_order INT NOT NULL COMMENT '节点顺序',
    node_name VARCHAR(50) NOT NULL COMMENT '节点名称',
    approver_id BIGINT COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人姓名',
    node_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING待处理,APPROVED已通过,REJECTED已拒绝',
    approve_type VARCHAR(20) COMMENT '审批类型:APPROVE同意,REJECT拒绝',
    comment TEXT COMMENT '审批意见',
    action_time DATETIME COMMENT '操作时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record_id (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 抄送记录表
CREATE TABLE oa_approval_cc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT '记录ID',
    cc_user_id BIGINT NOT NULL COMMENT '抄送人ID',
    cc_user_name VARCHAR(50) COMMENT '抄送人姓名',
    notify_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '通知状态',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    read_time DATETIME COMMENT '阅读时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record_id (record_id),
    INDEX idx_cc_user_id (cc_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 8.2 审批记录API接口

```java
/**
 * 审批记录Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/record")
@Tag(name = "审批记录", description = "审批记录相关接口")
public class ApprovalRecordController {

    @GetMapping("/my-apply")
    @Operation(summary = "我发起的申请")
    public Result<PageResult<ApprovalRecordVO>> getMyApply(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(recordService.getMyApply(pageNum, pageSize, businessType, status, startDate, endDate, userId));
    }

    @GetMapping("/my-approved")
    @Operation(summary = "我审批过的申请")
    public Result<PageResult<ApprovalRecordVO>> getMyApproved(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(recordService.getMyApproved(pageNum, pageSize, businessType, status, startDate, endDate, userId));
    }

    @GetMapping("/all")
    @Operation(summary = "全部审批记录(管理员)")
    public Result<PageResult<ApprovalRecordVO>> getAllRecord(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long deptId) {
        return Result.success(recordService.getAllRecord(pageNum, pageSize, businessType, status, startDate, endDate, userId, deptId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "审批记录详情")
    public Result<ApprovalRecordDetailVO> getRecordDetail(@PathVariable Long id) {
        return Result.success(recordService.getRecordDetail(id));
    }

    @GetMapping("/statistics")
    @Operation(summary = "审批统计")
    public Result<ApprovalStatisticsVO> getStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(recordService.getStatistics(startDate, endDate, deptId, userId));
    }

    @GetMapping("/export")
    @Operation(summary = "导出审批记录")
    public void exportRecord(
            @RequestParam String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) {
        recordService.exportRecord(businessType, status, startDate, endDate, deptId, response);
    }

    // ========== 抄送相关 ==========
    @GetMapping("/cc/my")
    @Operation(summary = "我的抄送列表")
    public Result<PageResult<ApprovalRecordVO>> getMyCc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Boolean isRead,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        return Result.success(recordService.getMyCc(pageNum, pageSize, isRead, userId));
    }

    @PutMapping("/cc/{id}/read")
    @Operation(summary = "标记抄送已读")
    public Result<Void> markCcRead(@PathVariable Long id,
                                    @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        recordService.markCcRead(id, userId);
        return Result.success();
    }
}
```

### 8.3 审批记录VO

```java
/**
 * 审批记录VO
 */
@Data
public class ApprovalRecordVO {
    private Long id;
    private String recordNo;
    private String businessType;
    private String businessTypeName;
    private Long businessId;
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private String title;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String statusName;
    private String currentNode;
    private Integer totalNodes;
    private Integer completedNodes;
    private String progress;
    private LocalDateTime createTime;
    private String createTimeStr;
}

/**
 * 审批记录详情VO
 */
@Data
public class ApprovalRecordDetailVO extends ApprovalRecordVO {
    private Object content;
    private List<ApprovalNodeDetailVO> nodes;
    private List<ApprovalCcVO> ccs;
}

/**
 * 审批节点明细VO
 */
@Data
public class ApprovalNodeDetailVO {
    private Long id;
    private Integer nodeOrder;
    private String nodeName;
    private Long approverId;
    private String approverName;
    private String nodeStatus;
    private String approveType;
    private String comment;
    private LocalDateTime actionTime;
}

/**
 * 抄送VO
 */
@Data
public class ApprovalCcVO {
    private Long id;
    private Long ccUserId;
    private String ccUserName;
    private String isRead;
    private LocalDateTime readTime;
}
```

---

## 9. 系统管理菜单设计

### 9.1 用户管理功能

```java
/**
 * 用户管理Controller
 */
@RestController
@RequestMapping("/api/v1/system/users")
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserManagementController {

    @GetMapping
    @Operation(summary = "用户列表")
    public Result<PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String status) {
        return Result.success(userService.listUsers(pageNum, pageSize, keyword, deptId, status));
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public Result<Long> createUser(@Valid @RequestBody UserForm form) {
        return Result.success(userService.createUser(form));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改用户")
    public Result<Void> updateUser(@PathVariable Long id,
                                    @Valid @RequestBody UserForm form) {
        userService.updateUser(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/password/reset")
    @Operation(summary = "重置密码")
    public Result<String> resetPassword(@PathVariable Long id) {
        return Result.success(userService.resetPassword(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestParam String status) {
        userService.updateUserStatus(id, status);
        return Result.success();
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入用户")
    public Result<ImportResultVO> importUsers(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.importUsers(file));
    }

    @GetMapping("/export")
    @Operation(summary = "导出用户")
    public void exportUsers(@RequestParam(required = false) Long deptId,
                             HttpServletResponse response) {
        userService.exportUsers(deptId, response);
    }

    @PostMapping("/dingtalk/bind")
    @Operation(summary = "绑定钉钉账号")
    public Result<Void> bindDingtalk(@RequestParam Long userId,
                                      @RequestParam String dingtalkUserId) {
        userService.bindDingtalk(userId, dingtalkUserId);
        return Result.success();
    }
}
```

### 9.2 部门管理功能

```java
/**
 * 部门管理Controller
 */
@RestController
@RequestMapping("/api/v1/system/depts")
@Tag(name = "部门管理", description = "部门管理相关接口")
public class DeptManagementController {

    @GetMapping("/tree")
    @Operation(summary = "部门树")
    public Result<List<DeptTreeVO>> getDeptTree() {
        return Result.success(deptService.getDeptTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "部门详情")
    public Result<DeptVO> getDeptById(@PathVariable Long id) {
        return Result.success(deptService.getDeptById(id));
    }

    @PostMapping
    @Operation(summary = "新增部门")
    public Result<Long> createDept(@Valid @RequestBody DeptForm form) {
        return Result.success(deptService.createDept(form));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改部门")
    public Result<Void> updateDept(@PathVariable Long id,
                                     @Valid @RequestBody DeptForm form) {
        deptService.updateDept(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门")
    public Result<Void> deleteDept(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success();
    }

    @PutMapping("/{id}/leader")
    @Operation(summary = "设置部门负责人")
    public Result<Void> setDeptLeader(@PathVariable Long id,
                                       @RequestParam Long leaderId) {
        deptService.setDeptLeader(id, leaderId);
        return Result.success();
    }

    @PostMapping("/dingtalk/sync")
    @Operation(summary = "同步钉钉部门")
    public Result<Void> syncDingtalkDept() {
        deptService.syncDingtalkDept();
        return Result.success();
    }
}
```

### 9.3 角色权限管理

```java
/**
 * 角色权限Controller
 */
@RestController
@RequestMapping("/api/v1/system/roles")
@Tag(name = "角色权限", description = "角色权限管理相关接口")
public class RolePermissionController {

    @GetMapping
    @Operation(summary = "角色列表")
    public Result<List<RoleVO>> listRoles() {
        return Result.success(roleService.listRoles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "角色详情")
    public Result<RoleVO> getRoleById(@PathVariable Long id) {
        return Result.success(roleService.getRoleById(id));
    }

    @PostMapping
    @Operation(summary = "新增角色")
    public Result<Long> createRole(@Valid @RequestBody RoleForm form) {
        return Result.success(roleService.createRole(form));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改角色")
    public Result<Void> updateRole(@PathVariable Long id,
                                    @Valid @RequestBody RoleForm form) {
        roleService.updateRole(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色权限")
    public Result<List<PermissionVO>> getRolePermissions(@PathVariable Long id) {
        return Result.success(roleService.getRolePermissions(id));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "分配角色权限")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                          @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }

    @GetMapping("/permissions")
    @Operation(summary = "获取权限树")
    public Result<List<PermissionTreeVO>> getPermissionTree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户角色")
    public Result<List<RoleVO>> getUserRoles(@PathVariable Long userId) {
        return Result.success(roleService.getUserRoles(userId));
    }

    @PutMapping("/user/{userId}")
    @Operation(summary = "分配用户角色")
    public Result<Void> assignUserRoles(@PathVariable Long userId,
                                        @RequestBody List<Long> roleIds) {
        roleService.assignUserRoles(userId, roleIds);
        return Result.success();
    }
}
```

### 9.4 考勤规则管理

```java
/**
 * 考勤规则Controller
 */
@RestController
@RequestMapping("/api/v1/system/attendance")
@Tag(name = "考勤规则", description = "考勤规则管理相关接口")
public class AttendanceRuleController {

    // ========== 班次管理 ==========
    @GetMapping("/shift")
    @Operation(summary = "班次列表")
    public Result<List<ShiftVO>> listShifts() {
        return Result.success(attendanceRuleService.listShifts());
    }

    @PostMapping("/shift")
    @Operation(summary = "新增班次")
    public Result<Long> createShift(@Valid @RequestBody ShiftForm form) {
        return Result.success(attendanceRuleService.createShift(form));
    }

    @PutMapping("/shift/{id}")
    @Operation(summary = "修改班次")
    public Result<Void> updateShift(@PathVariable Long id,
                                     @Valid @RequestBody ShiftForm form) {
        attendanceRuleService.updateShift(id, form);
        return Result.success();
    }

    @DeleteMapping("/shift/{id}")
    @Operation(summary = "删除班次")
    public Result<Void> deleteShift(@PathVariable Long id) {
        attendanceRuleService.deleteShift(id);
        return Result.success();
    }

    // ========== 考勤组管理 ==========
    @GetMapping("/group")
    @Operation(summary = "考勤组列表")
    public Result<PageResult<AttendanceGroupVO>> listGroups(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(attendanceRuleService.listGroups(pageNum, pageSize));
    }

    @PostMapping("/group")
    @Operation(summary = "新增考勤组")
    public Result<Long> createGroup(@Valid @RequestBody AttendanceGroupForm form) {
        return Result.success(attendanceRuleService.createGroup(form));
    }

    @PutMapping("/group/{id}")
    @Operation(summary = "修改考勤组")
    public Result<Void> updateGroup(@PathVariable Long id,
                                     @Valid @RequestBody AttendanceGroupForm form) {
        attendanceRuleService.updateGroup(id, form);
        return Result.success();
    }

    @DeleteMapping("/group/{id}")
    @Operation(summary = "删除考勤组")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        attendanceRuleService.deleteGroup(id);
        return Result.success();
    }

    // ========== 规则配置 ==========
    @GetMapping("/rule")
    @Operation(summary = "获取规则配置")
    public Result<Map<String, Object>> getRules() {
        return Result.success(attendanceRuleService.getRules());
    }

    @PutMapping("/rule/{ruleCode}")
    @Operation(summary = "更新规则配置")
    public Result<Void> updateRule(@PathVariable String ruleCode,
                                    @RequestBody Object config) {
        attendanceRuleService.updateRule(ruleCode, config);
        return Result.success();
    }

    // ========== 假期余额初始化 ==========
    @PostMapping("/leave-balance/init")
    @Operation(summary = "初始化年度假期余额")
    public Result<Void> initAnnualLeaveBalance(@RequestParam Integer year) {
        attendanceRuleService.initAnnualLeaveBalance(year);
        return Result.success();
    }
}
```

### 9.5 系统配置管理

```java
/**
 * 系统配置Controller
 */
@RestController
@RequestMapping("/api/v1/system/config")
@Tag(name = "系统配置", description = "系统配置相关接口")
public class SystemConfigController {

    @GetMapping
    @Operation(summary = "获取配置项")
    public Result<Map<String, String>> getConfig(@RequestParam(required = false) String group) {
        return Result.success(configService.getConfig(group));
    }

    @PutMapping("/{key}")
    @Operation(summary = "修改配置项")
    public Result<Void> updateConfig(@PathVariable String key,
                                     @RequestParam String value) {
        configService.updateConfig(key, value);
        return Result.success();
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新配置缓存")
    public Result<Void> refreshConfig() {
        configService.refreshConfig();
        return Result.success();
    }

    // ========== 数据字典 ==========
    @GetMapping("/dict")
    @Operation(summary = "获取字典列表")
    public Result<PageResult<DictVO>> listDict(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String type) {
        return Result.success(configService.listDict(pageNum, pageSize, type));
    }

    @GetMapping("/dict/{type}")
    @Operation(summary = "获取字典项")
    public Result<List<DictItemVO>> getDictItems(@PathVariable String type) {
        return Result.success(configService.getDictItems(type));
    }

    @PostMapping("/dict")
    @Operation(summary = "新增字典项")
    public Result<Long> createDictItem(@Valid @RequestBody DictItemForm form) {
        return Result.success(configService.createDictItem(form));
    }

    @PutMapping("/dict/{id}")
    @Operation(summary = "修改字典项")
    public Result<Void> updateDictItem(@PathVariable Long id,
                                         @Valid @RequestBody DictItemForm form) {
        configService.updateDictItem(id, form);
        return Result.success();
    }

    @DeleteMapping("/dict/{id}")
    @Operation(summary = "删除字典项")
    public Result<Void> deleteDictItem(@PathVariable Long id) {
        configService.deleteDictItem(id);
        return Result.success();
    }
}
```

---

## 10. 附录

### 10.1 数据库变更汇总

| 序号 | 表名 | 操作 | 说明 |
|------|------|------|------|
| 1 | oa_attendance_rule | CREATE | 考勤规则配置表 |
| 2 | oa_attendance_shift | CREATE | 班次配置表 |
| 3 | oa_attendance_group | CREATE | 考勤组表 |
| 4 | oa_attendance_holiday | CREATE | 节假日配置表 |
| 5 | oa_overtime | CREATE | 加班申请表 |
| 6 | oa_overtime_break | CREATE | 加班调休记录表 |
| 7 | oa_repair_card | CREATE | 补卡申请表 |
| 8 | oa_repair_card_statistics | CREATE | 补卡次数统计表 |
| 9 | oa_business_trip | CREATE | 出差申请表 |
| 10 | oa_business_trip_subsidy | CREATE | 出差补贴记录表 |
| 11 | oa_leave_type | CREATE | 假期类型表 |
| 12 | oa_leave_balance | CREATE | 员工假期余额表 |
| 13 | oa_leave | ALTER | 扩展请假申请表 |
| 14 | oa_salary | CREATE | 工资单主表 |
| 15 | oa_salary_item | CREATE | 工资项配置表 |
| 16 | oa_salary_notice | CREATE | 工资条发送记录表 |
| 17 | oa_salary_approval | CREATE | 工资审批记录表 |
| 18 | oa_approval_record | CREATE | 审批记录表(统一) |
| 19 | oa_approval_node_detail | CREATE | 审批节点明细表 |
| 20 | oa_approval_cc | CREATE | 抄送记录表 |

### 10.2 状态机流转

```
┌─────────────────────────────────────────────────────────────────┐
│                    加班申请状态机                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  DRAFT(草稿) ──▶ PENDING(待审批) ──▶ APPROVED(已审批)          │
│       │                │                   │                    │
│       │                │                   └──▶ REJECTED(已拒绝)│
│       │                │                                         │
│       │                ▼                                         │
│       └──────▶ CANCELLED(已撤回)                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    补卡申请状态机                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  PENDING(待审批) ──▶ APPROVED(已通过)                           │
│        │                    │                                    │
│        │                    └──▶ REJECTED(已拒绝)               │
│        │                                                         │
│        └──▶ CANCELLED(已撤回)                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    工资单状态机                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  DRAFT(草稿) ──▶ PENDING(审批中) ──▶ APPROVED(已审批)          │
│       │                │                    │                   │
│       │                │                    └──▶ REJECTED(已驳回)│
│       │                │                                    │    │
│       │                ▼                                    ▼    │
│       └──────▶ CANCELLED(已撤回)                PAID(已发放)    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 10.3 管理员可配置规则

所有业务规则均可由管理员在后台配置修改，系统自动计算执行：

| 功能 | 默认规则 | 管理员可配置 |
|------|----------|--------------|
| **加班审批** | 必须审批 | ✅ 可开关 |
| **加班时长** | 以审批单为准 | ✅ |
| **调休比例** | 统一1:1 | ✅ 可修改 |
| **调休有效期** | 6个月 | ✅ 可配置 |
| **补卡次数** | 每月5次 | ✅ 可配置 |
| **补卡时限** | 31个工作日 | ✅ 可配置 |
| **假期余额** | 按工龄计算 | ✅ 可修改规则 |

---

### 10.4 自动计算机制

系统自动计算执行，无需人工干预：

| 设计项 | 自动计算方案 | 触发时机 |
|--------|--------------|----------|
| **假期余额** | 按工龄自动计算年假天数 | 员工入职时、年终自动初始化 |
| **补卡次数** | Redis计数器自动校验 | 补卡申请提交时 |
| **加班调休** | 按加班类型自动计算调休时长 | 加班审批通过时 |
| **工资个税** | 累计预扣法自动计算 | 工资单生成时 |
| **考勤异常** | 打卡记录与班次对比自动检测 | 每日打卡结束后 |
| **历史归档** | 定时任务自动归档 | 每月1日凌晨 |

**示例代码：**

```java
// 假期余额自动计算（SSD文档5.2.2节）
public static int calculateAnnualLeave(int workYears) {
    if (workYears < 1) return 0;
    if (workYears < 5) return 5;
    if (workYears < 10) return 10;
    if (workYears < 20) return 15;
    return 20;
}

// 补卡次数Redis校验（SSD文档3.2.2节）
public void validateRepairCard(RepairCardForm form, Long userId) {
    String key = "repair:count:" + userId + ":" + yearMonth;
    Long count = redisTemplate.opsForValue().increment(key);
    if (count > ruleConfig.getMaxRepairCount()) {
        redisTemplate.opsForValue().decrement(key);
        throw new BusinessException("本月补卡次数已达上限");
    }
}
```

---

**文档版本历史：**

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|----------|------|
| V1.3 | 2026-05-26 | 新增考勤规则、加班、补卡、出差、假期、工资管理详细设计 | SolidOA Architecture Team |
| V1.2 | 2026-05-26 | 增加钉钉考勤集成详细设计 | SolidOA Architecture Team |
| V1.1 | 2026-05-15 | 增加用印、采购申请详细设计 | SolidOA Architecture Team |
| V1.0 | 2026-05-10 | 初始版本，包含核心模块设计 | SolidOA Architecture Team |