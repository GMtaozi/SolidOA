package com.solidoa.attendance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.attendance.form.CheckForm;
import com.solidoa.attendance.vo.AttendanceVO;
import com.solidoa.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/check")
    public Result<AttendanceVO> check(@RequestBody CheckForm form,
                                       @RequestHeader("X-User-Id") Long userId) {
        return Result.success(attendanceService.check(form, userId));
    }

    @GetMapping("/records")
    public Result<PageVO<AttendanceVO>> records(PageDTO dto,
                                                   @RequestParam(required = false) String checkDate,
                                                   @RequestHeader("X-User-Id") Long userId) {
        return Result.success(attendanceService.getRecords(dto, checkDate, userId));
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam String yearMonth,
                                                @RequestHeader("X-User-Id") Long userId) {
        return Result.success(attendanceService.getSummary(yearMonth, userId));
    }
}