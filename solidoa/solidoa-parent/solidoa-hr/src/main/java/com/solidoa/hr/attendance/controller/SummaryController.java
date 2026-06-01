package com.solidoa.hr.attendance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.hr.attendance.service.SummaryService;
import com.solidoa.hr.attendance.vo.SummaryVO;
import com.solidoa.hr.attendance.vo.ExceptionVO;
import com.solidoa.common.vo.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/attendance")
public class SummaryController {

    @Autowired
    private SummaryService summaryService;

    @GetMapping("/summary")
    public Result<SummaryVO> getSummary(@RequestParam String yearMonth,
                                        @RequestHeader("X-User-Id") Long userId) {
        return Result.success(summaryService.getSummary(yearMonth, userId));
    }

    @GetMapping("/month-summary")
    public Result<PageVO<SummaryVO>> getMonthSummary(@RequestParam String yearMonth,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(summaryService.getMonthSummary(yearMonth, pageNum, pageSize));
    }

    @GetMapping("/exceptions")
    public Result<List<ExceptionVO>> getExceptions(@RequestParam String yearMonth,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(summaryService.getExceptions(yearMonth, pageNum, pageSize));
    }

    /**
     * 获取指定用户指定月份的考勤汇总（供其他服务 Feign 调用）
     */
    @GetMapping("/user/{userId}/month/{yearMonth}/summary")
    public Result<Map<String, Object>> getAttendanceSummary(@PathVariable Long userId,
                                                              @PathVariable String yearMonth) {
        Map<String, Object> result = new HashMap<>();
        SummaryVO summary = summaryService.getSummary(yearMonth, userId);
        if (summary != null) {
            result.put("workDays", summary.getWorkDays());
            result.put("actualDays", summary.getActualDays());
            result.put("leaveDays", summary.getLeaveDays());
            result.put("attendanceRate", calculateAttendanceRate(summary));
        }
        return Result.success(result);
    }

    private double calculateAttendanceRate(SummaryVO summary) {
        if (summary.getWorkDays() == null || summary.getWorkDays() == 0) {
            return 100.0;
        }
        int actualDays = summary.getActualDays() != null ? summary.getActualDays() : 0;
        return Math.min(100.0, (actualDays * 100.0) / summary.getWorkDays());
    }
}