package com.solidoa.hr.attendance.controller;

import com.solidoa.hr.attendance.form.AdjustLeaveBalanceForm;
import com.solidoa.hr.attendance.form.InitLeaveBalanceForm;
import com.solidoa.hr.attendance.service.LeaveBalanceService;
import com.solidoa.hr.attendance.vo.LeaveBalanceVO;
import com.solidoa.hr.attendance.vo.LeaveTypeVO;
import com.solidoa.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 假期管理控制器
 */
@RestController
@RequestMapping("/api/v1/hr/attendance/leave")
@Slf4j
public class LeaveBalanceController {

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    /**
     * 获取假期类型列表
     */
    @GetMapping("/types")
    public Result<List<LeaveTypeVO>> getLeaveTypes() {
        return Result.success(leaveBalanceService.getLeaveTypes());
    }

    /**
     * 新增假期类型
     */
    @PostMapping("/types")
    public Result<Long> createLeaveType(@RequestBody java.util.Map<String, Object> form) {
        return Result.success(leaveBalanceService.createLeaveType(form));
    }

    /**
     * 修改假期类型
     */
    @PutMapping("/types/{id}")
    public Result<Void> updateLeaveType(@PathVariable Long id,
                                         @RequestBody java.util.Map<String, Object> form) {
        leaveBalanceService.updateLeaveType(id, form);
        return Result.success();
    }

    /**
     * 删除假期类型
     */
    @DeleteMapping("/types/{id}")
    public Result<Void> deleteLeaveType(@PathVariable Long id) {
        leaveBalanceService.deleteLeaveType(id);
        return Result.success();
    }

    /**
     * 获取我的假期余额
     */
    @GetMapping("/balance")
    public Result<List<LeaveBalanceVO>> getMyLeaveBalance(
            @RequestParam(required = false) Integer year,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(leaveBalanceService.getLeaveBalance(userId, year));
    }

    /**
     * 获取员工假期余额(管理员)
     */
    @GetMapping("/balance/{userId}")
    public Result<List<LeaveBalanceVO>> getUserLeaveBalance(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year) {
        return Result.success(leaveBalanceService.getLeaveBalance(userId, year));
    }

    /**
     * 初始化员工假期余额(管理员)
     */
    @PostMapping("/balance/init")
    public Result<Void> initLeaveBalance(@RequestBody InitLeaveBalanceForm form,
                                          @RequestHeader("X-User-Id") Long adminId) {
        log.info("初始化员工假期余额: adminId={}, userCount={}, year={}",
            adminId, form.getUserIds() != null ? form.getUserIds().size() : 0, form.getYear());
        leaveBalanceService.initLeaveBalance(form, adminId);
        return Result.success();
    }

    /**
     * 调整员工假期余额(管理员)
     */
    @PostMapping("/balance/adjust")
    public Result<Void> adjustLeaveBalance(@RequestBody AdjustLeaveBalanceForm form,
                                            @RequestHeader("X-User-Id") Long adminId) {
        log.info("调整员工假期余额: adminId={}, userId={}, leaveType={}, adjustType={}",
            adminId, form.getUserId(), form.getLeaveType(), form.getAdjustType());
        leaveBalanceService.adjustLeaveBalance(form, adminId);
        return Result.success();
    }
}