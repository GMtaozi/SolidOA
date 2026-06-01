package com.solidoa.hr.finance.service;

import com.solidoa.hr.finance.vo.DeptExpenseReportVO;
import java.util.List;
import java.util.Map;

public interface ReportService {
    List<DeptExpenseReportVO> getDeptExpenseReport(Integer year, Integer month);

    String exportExpenseReportAsync(Integer year, Integer month, Long userId);

    Map<String, Object> getExportProgress(String taskId);

    void exportExpenseReport(String taskId, Integer year, Integer month, Long userId);
}