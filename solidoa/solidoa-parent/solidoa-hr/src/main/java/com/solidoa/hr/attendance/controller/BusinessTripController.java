package com.solidoa.hr.attendance.controller;

import com.solidoa.hr.attendance.form.BusinessTripForm;
import com.solidoa.hr.attendance.service.BusinessTripService;
import com.solidoa.hr.attendance.vo.BusinessTripVO;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 出差申请控制器
 */
@RestController
@RequestMapping("/api/v1/hr/attendance/business-trip")
@Slf4j
public class BusinessTripController {

    @Autowired
    private BusinessTripService businessTripService;

    @PostMapping
    public Result<Long> createBusinessTrip(@Valid @RequestBody BusinessTripForm form,
                                           @RequestHeader("X-User-Id") Long userId) {
        return Result.success(businessTripService.createBusinessTrip(form, userId));
    }

    @GetMapping
    public Result<PageVO<BusinessTripVO>> listBusinessTrip(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(businessTripService.listBusinessTrip(
                userId, pageNum, pageSize, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result<BusinessTripVO> getBusinessTripById(@PathVariable Long id) {
        return Result.success(businessTripService.getBusinessTripById(id));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approveBusinessTrip(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") Long approverId) {
        businessTripService.approveBusinessTrip(id, body.get("comment"), body.get("approveResult"), approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancelBusinessTrip(@PathVariable Long id,
                                            @RequestHeader("X-User-Id") Long userId) {
        businessTripService.cancelBusinessTrip(id, userId);
        return Result.success();
    }

    /**
     * 获取当前用户已通过的出差申请列表（用于报销关联）
     */
    @GetMapping("/approved")
    public Result<List<BusinessTripVO>> getApprovedBusinessTrips(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(businessTripService.listApprovedBusinessTrips(userId));
    }
}