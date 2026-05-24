package com.solidoa.attendance.service.impl;

import com.solidoa.attendance.entity.Attendance;
import com.solidoa.attendance.mapper.AttendanceMapper;
import com.solidoa.attendance.mapper.AttendanceSummaryMapper;
import com.solidoa.attendance.service.SummaryService;
import com.solidoa.attendance.vo.ExceptionVO;
import com.solidoa.attendance.vo.SummaryVO;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        int workDays = endDate.getDayOfMonth();
        int actualDays = (int) records.stream()
            .filter(r -> "SIGN_IN".equals(r.getCheckType()))
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
        vo.setOvertimeHours(0.0);

        log.info("生成考勤汇总: userId={}, yearMonth={}, actualDays={}", userId, yearMonth, actualDays);
        return vo;
    }

    @Override
    public PageVO<SummaryVO> getMonthSummary(String yearMonth, Integer pageNum, Integer pageSize) {
        PageVO<SummaryVO> page = new PageVO<>();
        page.setRecords(new ArrayList<>());
        page.setTotal(0);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    @Override
    public List<ExceptionVO> getExceptions(String yearMonth, Integer pageNum, Integer pageSize) {
        List<ExceptionVO> exceptions = new ArrayList<>();

        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        log.info("查询考勤异常: yearMonth={}", yearMonth);
        return exceptions;
    }
}