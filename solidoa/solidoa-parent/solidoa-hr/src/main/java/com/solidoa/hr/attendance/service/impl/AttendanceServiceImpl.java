package com.solidoa.hr.attendance.service.impl;

import com.solidoa.hr.attendance.form.CheckForm;
import com.solidoa.hr.attendance.vo.AttendanceVO;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.hr.attendance.service.AttendanceService;
import com.solidoa.hr.attendance.mapper.AttendanceMapper;
import com.solidoa.hr.attendance.entity.Attendance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 考勤服务实现
 *
 * 考勤数据来源说明：
 * - 主数据源：钉钉同步（通过 DingTalk 服务）
 * - 本地打卡功能已禁用，保留 check 方法作为备用模式
 */
@Service
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    // 公司经纬度（从配置读取）
    @Value("${attendance.company.longitude:120.123456}")
    private BigDecimal companyLongitude;

    @Value("${attendance.company.latitude:30.123456}")
    private BigDecimal companyLatitude;

    @Value("${attendance.company.range:500}")
    private Integer locationRangeMeters;

    @Autowired
    private AttendanceMapper attendanceMapper;

    /**
     * 本地打卡功能（已禁用）
     * 保留代码作为备用模式
     * 如需启用，请取消注释
     */
    // @Override
    // @Transactional
    // public AttendanceVO check(CheckForm form, Long userId) {
    //     LocalDateTime now = LocalDateTime.now();
    //     LocalDate today = now.toLocalDate();
    //
    //     // 校验位置范围
    //     if (form.getLongitude() != null && form.getLatitude() != null) {
    //         double distance = calculateDistance(
    //             companyLongitude.doubleValue(), companyLatitude.doubleValue(),
    //             form.getLongitude().doubleValue(), form.getLatitude().doubleValue()
    //         );
    //
    //         if (distance > locationRangeMeters) {
    //             log.warn("打卡位置超出范围: userId={}, 距离={}米, 限制={}米",
    //                 userId, (int) distance, locationRangeMeters);
    //             throw new BusinessException("打卡位置距离公司超过" + locationRangeMeters + "米，请到公司范围内打卡");
    //         }
    //     }
    //
    //     // 校验设备（防止同一设备多账号）
    //     if (form.getDeviceId() != null && !form.getDeviceId().isEmpty()) {
    //         int deviceUserCount = attendanceMapper.countDeviceUsersToday(form.getDeviceId(), today);
    //         if (deviceUserCount > 3) {
    //             log.warn("同一设备打卡用户过多: deviceId={}, count={}", form.getDeviceId(), deviceUserCount);
    //             throw new BusinessException("检测到异常打卡行为，请联系管理员");
    //         }
    //     }
    //
    //     // 判断签到还是签退
    //     Attendance lastRecord = attendanceMapper.selectLastRecord(userId, today);
    //     String checkType = (lastRecord == null || "SIGN_OUT".equals(lastRecord.getCheckType()))
    //         ? "SIGN_IN" : "SIGN_OUT";
    //
    //     // 判断是否迟到/早退
    //     Integer isLate = 0;
    //     Integer isEarlyLeave = 0;
    //     if ("SIGN_IN".equals(checkType) && now.toLocalTime().isAfter(LocalTime.of(9, 0))) {
    //         isLate = 1;
    //     } else if ("SIGN_OUT".equals(checkType) && now.toLocalTime().isBefore(LocalTime.of(18, 0))) {
    //         isEarlyLeave = 1;
    //     }
    //
    //     Attendance attendance = new Attendance();
    //     attendance.setUserId(userId);
    //     attendance.setCheckDate(today);
    //     attendance.setCheckType(checkType);
    //     attendance.setCheckTime(now);
    //     attendance.setLocation(form.getLocation());
    //     attendance.setLongitude(form.getLongitude());
    //     attendance.setLatitude(form.getLatitude());
    //     attendance.setDeviceType(form.getDeviceType() != null ? form.getDeviceType() : "APP");
    //     attendance.setDeviceId(form.getDeviceId());
    //     attendance.setIsLate(isLate);
    //     attendance.setIsEarlyLeave(isEarlyLeave);
    //     attendance.setCreateTime(now);
    //
    //     attendanceMapper.insert(attendance);
    //     log.info("本地考勤打卡: userId={}, type={}, location={}", userId, checkType, form.getLocation());
    //
    //     AttendanceVO vo = new AttendanceVO();
    //     vo.setId(attendance.getId());
    //     vo.setCheckType(checkType);
    //     vo.setCheckTime(now.toString());
    //     vo.setLocation(form.getLocation());
    //     vo.setIsLate(isLate);
    //     vo.setIsEarlyLeave(isEarlyLeave);
    //     return vo;
    // }

    @Override
    public PageVO<AttendanceVO> getRecords(PageDTO dto, String checkDate, Long userId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        LocalDate date = checkDate != null ? LocalDate.parse(checkDate) : LocalDate.now();
        List<AttendanceVO> records = attendanceMapper.selectPageList(offset, dto.getPageSize(), userId, date);
        long total = attendanceMapper.selectCount(userId, date);

        PageVO<AttendanceVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(dto.getPageNum());
        page.setPageSize(dto.getPageSize());
        return page;
    }

    @Override
    public Map<String, Object> getSummary(String yearMonth, Long userId) {
        List<Attendance> records = attendanceMapper.selectByMonth(userId, yearMonth);

        // 按 DISTINCT checkDate 统计出勤天数
        long totalDays = records.stream()
            .map(Attendance::getCheckDate)
            .distinct()
            .count();

        int lateDays = 0;
        int earlyLeaveDays = 0;

        for (Attendance a : records) {
            if (a.getIsLate() != null && a.getIsLate() == 1) lateDays++;
            if (a.getIsEarlyLeave() != null && a.getIsEarlyLeave() == 1) earlyLeaveDays++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("yearMonth", yearMonth);
        result.put("totalDays", totalDays);
        result.put("lateDays", lateDays);
        result.put("earlyLeaveDays", earlyLeaveDays);
        return result;
    }

    @Override
    public List<Map<String, Object>> getMergedClockRecords(Long userId, String startDate, String endDate) {
        /**
         * 获取合并后的打卡记录
         * 1. 优先从钉钉同步数据中获取（高优先级）
         * 2. 本地数据作为备用补充
         *
         * 注意：实际项目中应通过 Feign 调用 DingTalk 服务获取钉钉数据
         * 此处简化处理，仅返回本地数据作为示例
         */
        log.info("获取合并打卡记录: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        List<Map<String, Object>> records = new ArrayList<>();

        // 获取本地打卡记录
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now();
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        List<Attendance> localRecords = attendanceMapper.selectByDateRange(userId, start, end);

        // 合并到结果列表
        for (Attendance record : localRecords) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("userId", record.getUserId());
            item.put("clockTime", record.getCheckTime().toString());
            item.put("clockType", record.getCheckType());
            item.put("location", record.getLocation());
            item.put("source", "LOCAL"); // 本地数据标记
            item.put("isLate", record.getIsLate());
            item.put("isEarlyLeave", record.getIsEarlyLeave());
            records.add(item);
        }

        /**
         * TODO: 实际项目中应添加钉钉数据合并逻辑：
         *
         * 1. 通过 Feign 调用 DingTalk 服务
         * 2. 获取钉钉打卡记录
         * 3. 按时间合并去重
         * 4. 钉钉数据标记为 source = "DINGTALK"
         */

        // 按时间排序
        records.sort((a, b) -> {
            String timeA = (String) a.get("clockTime");
            String timeB = (String) b.get("clockTime");
            return timeB.compareTo(timeA); // 降序
        });

        return records;
    }

    /**
     * 计算两点之间的距离（米）
     * 使用 Haversine 公式
     */
    private double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}