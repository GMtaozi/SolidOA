package com.solidoa.attendance.service.impl;

import com.solidoa.attendance.form.CheckForm;
import com.solidoa.attendance.vo.AttendanceVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.attendance.service.AttendanceService;
import com.solidoa.attendance.mapper.AttendanceMapper;
import com.solidoa.attendance.entity.Attendance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Override
    @Transactional
    public AttendanceVO check(CheckForm form, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 判断签到还是签退
        Attendance lastRecord = attendanceMapper.selectLastRecord(userId, today);
        String checkType = (lastRecord == null || "SIGN_OUT".equals(lastRecord.getCheckType()))
            ? "SIGN_IN" : "SIGN_OUT";

        // 判断是否迟到/早退
        Integer isLate = 0;
        Integer isEarlyLeave = 0;
        if ("SIGN_IN".equals(checkType) && now.toLocalTime().isAfter(LocalTime.of(9, 0))) {
            isLate = 1;
        } else if ("SIGN_OUT".equals(checkType) && now.toLocalTime().isBefore(LocalTime.of(18, 0))) {
            isEarlyLeave = 1;
        }

        Attendance attendance = new Attendance();
        attendance.setUserId(userId);
        attendance.setCheckDate(today);
        attendance.setCheckType(checkType);
        attendance.setCheckTime(now);
        attendance.setLocation(form.getLocation());
        attendance.setLongitude(form.getLongitude());
        attendance.setLatitude(form.getLatitude());
        attendance.setDeviceType(form.getDeviceType() != null ? form.getDeviceType() : "APP");
        attendance.setIsLate(isLate);
        attendance.setIsEarlyLeave(isEarlyLeave);
        attendance.setCreateTime(now);

        attendanceMapper.insert(attendance);
        log.info("考勤打卡: userId={}, type={}, location={}", userId, checkType, form.getLocation());

        AttendanceVO vo = new AttendanceVO();
        vo.setId(attendance.getId());
        vo.setCheckType(checkType);
        vo.setCheckTime(now.toString());
        vo.setLocation(form.getLocation());
        vo.setIsLate(isLate);
        vo.setIsEarlyLeave(isEarlyLeave);
        return vo;
    }

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

        int totalDays = records.size() / 2; // 每天两次打卡
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
}