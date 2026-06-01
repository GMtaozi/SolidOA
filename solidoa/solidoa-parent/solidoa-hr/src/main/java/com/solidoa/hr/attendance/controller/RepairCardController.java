package com.solidoa.hr.attendance.controller;

import com.solidoa.hr.attendance.form.RepairForm;
import com.solidoa.hr.attendance.service.RepairCardService;
import com.solidoa.hr.attendance.vo.RepairVO;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 补卡申请Controller
 */
@RestController
@RequestMapping("/api/v1/hr/attendance/repair")
public class RepairCardController {

    @Autowired
    private RepairCardService repairCardService;

    /**
     * 新建补卡申请
     */
    @PostMapping
    public Result<Long> create(@RequestBody @Valid RepairForm form,
                                @RequestHeader("X-User-Id") Long userId) {
        return Result.success(repairCardService.create(form, userId));
    }

    /**
     * 补卡列表
     */
    @GetMapping
    public Result<PageVO<RepairVO>> list(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(repairCardService.pageList(userId, pageNum, pageSize));
    }

    /**
     * 补卡详情
     */
    @GetMapping("/{id}")
    public Result<RepairVO> getById(@PathVariable Long id) {
        return Result.success(repairCardService.getById(id));
    }

    /**
     * 审批补卡
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody Map<String, String> body,
                                @RequestHeader("X-User-Id") Long approverId) {
        repairCardService.approve(id, body.get("result"), approverId);
        return Result.success();
    }

    /**
     * 撤回补卡申请
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                                @RequestHeader("X-User-Id") Long userId) {
        repairCardService.cancel(id, userId);
        return Result.success();
    }

    /**
     * 补卡统计
     */
    @GetMapping("/statistics")
    public Result<Object> statistics(@RequestHeader("X-User-Id") Long userId,
                                      @RequestParam(required = false) String yearMonth) {
        return Result.success(repairCardService.getStatistics(userId, yearMonth));
    }

    /**
     * 获取待审批列表
     */
    @GetMapping("/pending")
    public Result<List<RepairVO>> getPending() {
        return Result.success(repairCardService.getPendingList());
    }
}