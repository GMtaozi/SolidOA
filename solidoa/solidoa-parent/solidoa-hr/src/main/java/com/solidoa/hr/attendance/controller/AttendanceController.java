package com.solidoa.hr.attendance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.security.UserContextHolder;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.hr.attendance.form.CheckForm;
import com.solidoa.hr.attendance.vo.AttendanceVO;
import com.solidoa.hr.attendance.service.AttendanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 考勤控制器
 *
 * 考勤数据来源说明：
 * - 主数据源：钉钉同步（通过 DingTalk 服务同步打卡数据）
 * - 本地打卡功能已禁用，保留代码作为备用模式
 *
 * 如需启用本地打卡功能：
 * 1. 取消注释 check 方法的 @PostMapping 注解和方法体
 * 2. 在 AttendanceServiceImpl 中启用 check 方法实现
 */
@RestController
@RequestMapping("/api/v1/hr/attendance")
@Slf4j
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * 本地打卡接口（已禁用）
     * 保留代码作为备用模式
     * 如需启用，请在方法上添加 @PostMapping("/check") 注解
     */
    // @PostMapping("/check")
    // public Result<AttendanceVO> check(@RequestBody CheckForm form,
    //                                    @RequestHeader("X-User-Id") Long userId) {
    //     log.info("本地打卡功能已禁用，请使用钉钉App打卡");
    //     return Result.success(attendanceService.check(form, userId));
    // }

    @GetMapping("/records")
    public Result<PageVO<AttendanceVO>> records(PageDTO dto,
                                                   @RequestParam(value = "checkDate", required = false) String checkDate) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(attendanceService.getRecords(dto, checkDate, userId));
    }

    @GetMapping("/summary-data")
    public Result<Map<String, Object>> summary(@RequestParam String yearMonth) {
        Long userId = UserContextHolder.getUserId();
        return Result.success(attendanceService.getSummary(yearMonth, userId));
    }

    /**
     * 获取考勤统计数据（合并钉钉数据）
     * 权限校验：仅能查询自己或下属的考勤数据
     */
    @GetMapping("/clock")
    public Result<List<Map<String, Object>>> getClockRecords(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        Long userId = UserContextHolder.getUserId();
        // 考勤数据权限校验：当前用户只能查看自己的考勤数据
        // 如需查看下属数据，应调用专门的统计接口
        return Result.success(attendanceService.getMergedClockRecords(
            userId, startDate, endDate
        ));
    }
}