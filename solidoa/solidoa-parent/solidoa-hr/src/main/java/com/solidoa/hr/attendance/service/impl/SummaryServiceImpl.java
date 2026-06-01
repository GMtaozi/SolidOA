package com.solidoa.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.hr.attendance.entity.Attendance;
import com.solidoa.hr.attendance.entity.AttendanceSummary;
import com.solidoa.hr.attendance.mapper.AttendanceMapper;
import com.solidoa.hr.attendance.mapper.AttendanceSummaryMapper;
import com.solidoa.hr.attendance.service.SummaryService;
import com.solidoa.hr.attendance.vo.ExceptionVO;
import com.solidoa.hr.attendance.vo.SummaryVO;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SummaryServiceImpl implements SummaryService {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private AttendanceSummaryMapper summaryMapper;

    @Override
    public SummaryVO getSummary(String yearMonth, Long userId) {
        SummaryVO vo = new SummaryVO();

        List<Attendance> records = attendanceMapper.selectByMonth(userId, yearMonth);

        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 计算工作日天数（排除周末）
        int workDays = (int) startDate.datesUntil(endDate.plusDays(1))
            .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY &&
                        d.getDayOfWeek() != DayOfWeek.SUNDAY)
            .count();

        // 按签到日期去重统计出勤天数，避免同一天多次签到被重复计算
        int actualDays = (int) records.stream()
            .filter(r -> "SIGN_IN".equals(r.getCheckType()))
            .map(Attendance::getCheckDate)
            .distinct()
            .count();
        int lateDays = (int) records.stream()
            .filter(r -> r.getIsLate() != null && r.getIsLate() == 1)
            .count();
        int earlyLeaveDays = (int) records.stream()
            .filter(r -> r.getIsEarlyLeave() != null && r.getIsEarlyLeave() == 1)
            .count();

        vo.setWorkDays(workDays);
        vo.setActualDays(actualDays);
        vo.setLateDays(lateDays);
        vo.setEarlyLeaveDays(earlyLeaveDays);
        vo.setLeaveDays(0);
        vo.setAbsentDays(Math.max(0, workDays - actualDays));
        vo.setBusinessDays(0);
        vo.setOvertimeHours(BigDecimal.ZERO);

        log.info("生成考勤汇总: userId={}, yearMonth={}, actualDays={}", userId, yearMonth, actualDays);
        return vo;
    }

    @Override
    public PageVO<SummaryVO> getMonthSummary(String yearMonth, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AttendanceSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceSummary::getYearMonth, yearMonth);

        Page<AttendanceSummary> page = new Page<>(pageNum, pageSize);
        Page<AttendanceSummary> result = summaryMapper.selectPage(page, wrapper);

        List<SummaryVO> voList = result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());

        PageVO<SummaryVO> pageVO = new PageVO<>();
        pageVO.setRecords(voList);
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(pageNum);
        pageVO.setPageSize(pageSize);
        return pageVO;
    }

    @Override
    public List<ExceptionVO> getExceptions(String yearMonth, Integer pageNum, Integer pageSize) {
        throw new UnsupportedOperationException("考勤异常查询功能尚未实现");
    }

    /**
     * 每月1日凌晨2点执行上月考勤统计
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void calculateLastMonthSummary() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        String yearMonth = lastMonth.getYear() + "-" + String.format("%02d", lastMonth.getMonthValue());

        log.info("开始生成上月考勤汇总: yearMonth={}", yearMonth);

        // 获取所有用户（分页查询避免一次性加载所有用户）
        // 实际实现应该通过 Feign 调用获取用户列表
        List<Long> userIds = getAllUserIds();
        if (userIds.isEmpty()) {
            log.info("没有用户需要生成考勤汇总");
            return;
        }

        // 批次处理，每批100个用户，防止内存溢出
        int batchSize = 100;
        int total = userIds.size();
        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<Long> batch = userIds.subList(i, end);
            for (Long userId : batch) {
                try {
                    generateUserSummary(userId, yearMonth);
                } catch (Exception e) {
                    log.error("生成用户考勤汇总失败: userId={}, yearMonth={}", userId, yearMonth, e);
                }
            }
            log.info("考勤汇总批次处理进度: {}/{}", end, total);
        }

        log.info("上月考勤汇总生成完成: yearMonth={}, userCount={}", yearMonth, userIds.size());
    }

    /**
     * 生成单个用户的考勤汇总
     */
    @Transactional
    public void generateUserSummary(Long userId, String yearMonth) {
        List<Attendance> records = attendanceMapper.selectByMonth(userId, yearMonth);

        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 计算工作日天数
        int workDays = (int) startDate.datesUntil(endDate.plusDays(1))
            .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY &&
                        d.getDayOfWeek() != DayOfWeek.SUNDAY)
            .count();

        // 按签到日期去重统计出勤天数，避免同一天多次签到被重复计算
        int actualDays = (int) records.stream()
            .filter(r -> "SIGN_IN".equals(r.getCheckType()))
            .map(Attendance::getCheckDate)
            .distinct()
            .count();
        int lateDays = (int) records.stream()
            .filter(r -> r.getIsLate() != null && r.getIsLate() == 1)
            .count();
        int earlyLeaveDays = (int) records.stream()
            .filter(r -> r.getIsEarlyLeave() != null && r.getIsEarlyLeave() == 1)
            .count();

        // 查找或创建汇总记录
        AttendanceSummary summary = summaryMapper.selectByUserAndMonth(userId, yearMonth);
        if (summary == null) {
            summary = new AttendanceSummary();
            summary.setUserId(userId);
            summary.setYearMonth(yearMonth);
        }

        summary.setWorkDays(workDays);
        summary.setActualDays(actualDays);
        summary.setLateCount(lateDays);
        summary.setEarlyLeaveCount(earlyLeaveDays);
        summary.setAbsentDays(BigDecimal.valueOf(Math.max(0, workDays - actualDays)));
        summary.setLeaveDays(BigDecimal.ZERO);
        summary.setBusinessDays(BigDecimal.ZERO);
        summary.setOvertimeHours(BigDecimal.ZERO);

        if (summary.getId() == null) {
            summaryMapper.insert(summary);
        } else {
            summaryMapper.updateById(summary);
        }
    }

    private List<Long> getAllUserIds() {
        // 实际应该通过 Feign 调用 system-service 获取用户列表
        // 这里从考勤表中查询所有不同的用户ID，避免全表加载
        return attendanceMapper.selectDistinctUserIds();
    }

    private SummaryVO convertToVO(AttendanceSummary summary) {
        SummaryVO vo = new SummaryVO();
        vo.setUserId(summary.getUserId());
        vo.setYearMonth(summary.getYearMonth());
        vo.setWorkDays(summary.getWorkDays() != null ? summary.getWorkDays() : 0);
        vo.setActualDays(summary.getActualDays() != null ? summary.getActualDays() : 0);
        vo.setLateDays(summary.getLateCount() != null ? summary.getLateCount() : 0);
        vo.setEarlyLeaveDays(summary.getEarlyLeaveCount() != null ? summary.getEarlyLeaveCount() : 0);
        vo.setLeaveDays(summary.getLeaveDays() != null ? summary.getLeaveDays().intValue() : 0);
        vo.setAbsentDays(summary.getAbsentDays() != null ? summary.getAbsentDays().intValue() : 0);
        vo.setBusinessDays(summary.getBusinessDays() != null ? summary.getBusinessDays().intValue() : 0);
        vo.setOvertimeHours(summary.getOvertimeHours() != null ? summary.getOvertimeHours() : BigDecimal.ZERO);
        return vo;
    }
}