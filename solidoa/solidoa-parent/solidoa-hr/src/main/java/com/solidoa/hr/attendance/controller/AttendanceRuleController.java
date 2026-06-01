package com.solidoa.hr.attendance.controller;

import com.solidoa.hr.attendance.form.AttendanceGroupForm;
import com.solidoa.hr.attendance.form.HolidayForm;
import com.solidoa.hr.attendance.form.RuleConfigForm;
import com.solidoa.hr.attendance.form.ShiftForm;
import com.solidoa.hr.attendance.service.AttendanceRuleService;
import com.solidoa.hr.attendance.vo.AttendanceGroupVO;
import com.solidoa.hr.attendance.vo.HolidayVO;
import com.solidoa.hr.attendance.vo.RuleConfigVO;
import com.solidoa.hr.attendance.vo.ShiftVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 考勤规则控制器
 */
@RestController
@RequestMapping("/api/v1/hr/attendance")
@Tag(name = "考勤规则", description = "考勤规则管理相关接口")
@Slf4j
@Validated
public class AttendanceRuleController {

    @Autowired
    private AttendanceRuleService attendanceRuleService;

    // ========== 班次管理 ==========

    /**
     * 班次列表
     */
    @GetMapping("/shift")
    @Operation(summary = "班次列表")
    public Result<List<ShiftVO>> listShifts() {
        return Result.success(attendanceRuleService.listShifts());
    }

    /**
     * 班次详情
     */
    @GetMapping("/shift/{id}")
    @Operation(summary = "班次详情")
    public Result<ShiftVO> getShiftById(@PathVariable Long id) {
        return Result.success(attendanceRuleService.getShiftById(id));
    }

    /**
     * 新增班次
     */
    @PostMapping("/shift")
    @Operation(summary = "新增班次")
    public Result<Long> createShift(@Valid @RequestBody ShiftForm form) {
        return Result.success(attendanceRuleService.createShift(form));
    }

    /**
     * 修改班次
     */
    @PutMapping("/shift/{id}")
    @Operation(summary = "修改班次")
    public Result<Void> updateShift(@PathVariable Long id,
                                     @Valid @RequestBody ShiftForm form) {
        attendanceRuleService.updateShift(id, form);
        return Result.success();
    }

    /**
     * 删除班次
     */
    @DeleteMapping("/shift/{id}")
    @Operation(summary = "删除班次")
    public Result<Void> deleteShift(@PathVariable Long id) {
        attendanceRuleService.deleteShift(id);
        return Result.success();
    }

    // ========== 考勤组管理 ==========

    /**
     * 考勤组列表
     */
    @GetMapping("/group")
    @Operation(summary = "考勤组列表")
    public Result<PageVO<AttendanceGroupVO>> listGroups(PageDTO dto) {
        return Result.success(attendanceRuleService.listGroups(dto));
    }

    /**
     * 考勤组详情
     */
    @GetMapping("/group/{id}")
    @Operation(summary = "考勤组详情")
    public Result<AttendanceGroupVO> getGroupById(@PathVariable Long id) {
        return Result.success(attendanceRuleService.getGroupById(id));
    }

    /**
     * 新增考勤组
     */
    @PostMapping("/group")
    @Operation(summary = "新增考勤组")
    public Result<Long> createGroup(@Valid @RequestBody AttendanceGroupForm form) {
        return Result.success(attendanceRuleService.createGroup(form));
    }

    /**
     * 修改考勤组
     */
    @PutMapping("/group/{id}")
    @Operation(summary = "修改考勤组")
    public Result<Void> updateGroup(@PathVariable Long id,
                                     @Valid @RequestBody AttendanceGroupForm form) {
        attendanceRuleService.updateGroup(id, form);
        return Result.success();
    }

    /**
     * 删除考勤组
     */
    @DeleteMapping("/group/{id}")
    @Operation(summary = "删除考勤组")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        attendanceRuleService.deleteGroup(id);
        return Result.success();
    }

    // ========== 规则配置 ==========

    /**
     * 获取规则配置
     */
    @GetMapping("/rule")
    @Operation(summary = "获取规则配置")
    public Result<Map<String, Object>> getRules() {
        return Result.success(attendanceRuleService.getRules());
    }

    /**
     * 获取指定规则配置
     */
    @GetMapping("/rule/{ruleType}")
    @Operation(summary = "获取指定规则配置")
    public Result<RuleConfigVO> getRuleByCode(@PathVariable String ruleType) {
        return Result.success(attendanceRuleService.getRuleByCode(ruleType));
    }

    /**
     * 更新规则配置
     */
    @PutMapping("/rule/{ruleType}")
    @Operation(summary = "更新规则配置")
    public Result<Void> updateRule(@PathVariable String ruleType,
                                    @RequestBody Map<String, Object> config) {
        attendanceRuleService.updateRule(ruleType, config);
        return Result.success();
    }

    /**
     * 批量更新规则配置
     */
    @PutMapping("/rule")
    @Operation(summary = "批量更新规则配置")
    public Result<Void> updateRules(@RequestBody Map<String, Object> rules) {
        attendanceRuleService.updateRules(rules);
        return Result.success();
    }

    // ========== 节假日管理 ==========

    /**
     * 节假日列表
     */
    @GetMapping("/holiday")
    @Operation(summary = "节假日列表")
    public Result<List<HolidayVO>> listHolidays(@RequestParam(required = false) Integer year) {
        return Result.success(attendanceRuleService.listHolidays(year));
    }

    /**
     * 调休上班日列表
     */
    @GetMapping("/workday")
    @Operation(summary = "调休上班日列表")
    public Result<List<HolidayVO>> listWorkdays(@RequestParam(required = false) Integer year) {
        return Result.success(attendanceRuleService.listWorkdays(year));
    }

    /**
     * 新增节假日
     */
    @PostMapping("/holiday")
    @Operation(summary = "新增节假日")
    public Result<Long> createHoliday(@Valid @RequestBody HolidayForm form) {
        return Result.success(attendanceRuleService.createHoliday(form));
    }

    /**
     * 修改节假日
     */
    @PutMapping("/holiday/{id}")
    @Operation(summary = "修改节假日")
    public Result<Void> updateHoliday(@PathVariable Long id,
                                       @Valid @RequestBody HolidayForm form) {
        attendanceRuleService.updateHoliday(id, form);
        return Result.success();
    }

    /**
     * 删除节假日
     */
    @DeleteMapping("/holiday/{id}")
    @Operation(summary = "删除节假日")
    public Result<Void> deleteHoliday(@PathVariable Long id) {
        attendanceRuleService.deleteHoliday(id);
        return Result.success();
    }

    /**
     * 批量导入节假日
     */
    @PostMapping("/holiday/import")
    @Operation(summary = "批量导入节假日")
    public Result<Void> importHolidays(@RequestBody List<HolidayForm> holidays) {
        attendanceRuleService.importHolidays(holidays);
        return Result.success();
    }

    // ========== 假期余额初始化 ==========

    /**
     * 初始化年度假期余额
     */
    @PostMapping("/leave-balance/init")
    @Operation(summary = "初始化年度假期余额")
    public Result<Void> initAnnualLeaveBalance(@RequestParam Integer year) {
        attendanceRuleService.initAnnualLeaveBalance(year);
        return Result.success();
    }
}