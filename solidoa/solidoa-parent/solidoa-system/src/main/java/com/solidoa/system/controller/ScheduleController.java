package com.solidoa.system.controller;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.result.Result;
import com.solidoa.system.form.ScheduleForm;
import com.solidoa.system.vo.ScheduleVO;
import com.solidoa.system.service.ScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system/schedules")
@Validated
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    private Long getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException(401, "缺少用户身份标识");
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "无效的用户身份标识");
        }
    }

    @PostMapping
    public Result<Long> create(@RequestBody ScheduleForm form, HttpServletRequest request) {
        return Result.success(scheduleService.create(form, getUserId(request)));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScheduleForm form, HttpServletRequest request) {
        scheduleService.update(id, form, getUserId(request));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        scheduleService.delete(id, getUserId(request));
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ScheduleVO> getById(@PathVariable Long id) {
        return Result.success(scheduleService.getById(id));
    }

    @GetMapping
    public Result<List<ScheduleVO>> list(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            HttpServletRequest request) {
        return Result.success(scheduleService.listByDateRange(startDate, endDate, getUserId(request)));
    }

    }