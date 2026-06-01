package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.vo.SummaryVO;
import com.solidoa.common.vo.PageVO;
import java.util.List;

public interface SummaryService {
    SummaryVO getSummary(String yearMonth, Long userId);

    PageVO<SummaryVO> getMonthSummary(String yearMonth, Integer pageNum, Integer pageSize);

    List<com.solidoa.hr.attendance.vo.ExceptionVO> getExceptions(String yearMonth, Integer pageNum, Integer pageSize);
}