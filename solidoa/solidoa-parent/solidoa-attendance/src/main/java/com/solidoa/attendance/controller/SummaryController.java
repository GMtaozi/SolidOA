package com.solidoa.attendance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.attendance.service.SummaryService;
import com.solidoa.attendance.vo.SummaryVO;
import com.solidoa.attendance.vo.ExceptionVO;
import com.solidoa.common.vo.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
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
}