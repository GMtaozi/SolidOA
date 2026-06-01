package com.solidoa.hr.attendance.controller;

import com.solidoa.hr.attendance.form.ApproveForm;
import com.solidoa.hr.attendance.form.OvertimeForm;
import com.solidoa.hr.attendance.service.OvertimeService;
import com.solidoa.hr.attendance.vo.OvertimeBalanceVO;
import com.solidoa.hr.attendance.vo.OvertimeVO;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/**
 * 加班申请控制器
 */
@RestController
@RequestMapping("/api/v1/hr/attendance/overtime")
@Slf4j
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    /**
     * 新建加班申请
     */
    @PostMapping
    public Result<Long> createOvertime(@RequestBody OvertimeForm form,
                                        @RequestHeader("X-User-Id") Long userId) {
        log.info("新建加班申请: userId={}, overtimeType={}", userId, form.getOvertimeType());
        return Result.success(overtimeService.createOvertime(form, userId));
    }

    /**
     * 加班列表
     */
    @GetMapping
    public Result<PageVO<OvertimeVO>> listOvertime(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String overtimeType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        PageDTO dto = new PageDTO();
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        return Result.success(overtimeService.listOvertime(dto, status, overtimeType, startDate, endDate));
    }

    /**
     * 加班详情
     */
    @GetMapping("/{id}")
    public Result<OvertimeVO> getOvertimeById(@PathVariable Long id,
                                              @RequestHeader("X-User-Id") Long userId) {
        return Result.success(overtimeService.getOvertimeById(id, userId));
    }

    /**
     * 审批加班
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approveOvertime(@PathVariable Long id,
                                        @RequestBody ApproveForm form,
                                        @RequestHeader("X-User-Id") Long approverId) {
        log.info("审批加班: id={}, approverId={}, approveType={}", id, approverId, form.getApproveType());
        overtimeService.approveOvertime(id, form, approverId);
        return Result.success();
    }

    /**
     * 撤回加班申请
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOvertime(@PathVariable Long id,
                                        @RequestHeader("X-User-Id") Long userId) {
        log.info("撤回加班申请: id={}, userId={}", id, userId);
        overtimeService.cancelOvertime(id, userId);
        return Result.success();
    }

    /**
     * 查询调休余额
     */
    @GetMapping("/balance")
    public Result<OvertimeBalanceVO> getOvertimeBalance(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(overtimeService.getOvertimeBalance(userId));
    }
}