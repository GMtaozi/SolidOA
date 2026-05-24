package com.solidoa.attendance.service;

import com.solidoa.attendance.form.CheckForm;
import com.solidoa.attendance.vo.AttendanceVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import java.util.Map;

public interface AttendanceService {
    AttendanceVO check(CheckForm form, Long userId);
    PageVO<AttendanceVO> getRecords(PageDTO dto, String checkDate, Long userId);
    Map<String, Object> getSummary(String yearMonth, Long userId);
}