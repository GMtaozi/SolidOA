package com.solidoa.hr.finance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.hr.finance.service.ReportService;
import com.solidoa.hr.finance.vo.DeptExpenseReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/finance/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/dept-expense")
    public Result<List<DeptExpenseReportVO>> getDeptExpenseReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.success(reportService.getDeptExpenseReport(year, month));
    }

    @PostMapping("/export")
    public Result<String> exportReport(@RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month,
                                        @RequestHeader("X-User-Id") Long userId) {
        String taskId = reportService.exportExpenseReportAsync(year, month, userId);
        return Result.success(taskId);
    }

    @GetMapping("/export/{taskId}/progress")
    public Result<Map<String, Object>> getExportProgress(@PathVariable String taskId) {
        return Result.success(reportService.getExportProgress(taskId));
    }
}