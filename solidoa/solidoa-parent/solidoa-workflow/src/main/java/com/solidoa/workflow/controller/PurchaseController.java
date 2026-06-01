package com.solidoa.workflow.controller;

import com.solidoa.common.result.Result;
import com.solidoa.workflow.form.PurchaseForm;
import com.solidoa.workflow.form.PurchaseProgressForm;
import com.solidoa.workflow.form.ApproveForm;
import com.solidoa.workflow.vo.PurchaseVO;
import com.solidoa.workflow.vo.PurchaseStatisticsVO;
import com.solidoa.workflow.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 采购申请Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/purchase")
@Slf4j
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid PurchaseForm form,
                               @RequestHeader("X-User-Id") Long userId) {
        return Result.success(purchaseService.createPurchase(form, userId));
    }

    @GetMapping
    public Result<List<PurchaseVO>> list(@RequestHeader("X-User-Id") Long userId,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String purchaseType) {
        return Result.success(purchaseService.listPurchase(userId, status, purchaseType));
    }

    @GetMapping("/{id}")
    public Result<PurchaseVO> getById(@PathVariable Long id,
                                       @RequestHeader("X-User-Id") Long userId) {
        return Result.success(purchaseService.getPurchaseById(id, userId));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                              @RequestBody ApproveForm form,
                              @RequestHeader("X-User-Id") Long approverId) {
        purchaseService.approvePurchase(id, form, approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                             @RequestHeader("X-User-Id") Long userId) {
        purchaseService.cancelPurchase(id, userId);
        return Result.success();
    }

    @PutMapping("/{id}/progress")
    public Result<Void> updateProgress(@PathVariable Long id,
                                      @RequestBody PurchaseProgressForm form,
                                      @RequestHeader("X-User-Id") Long operatorId) {
        purchaseService.updateProgress(id, form, operatorId);
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<PurchaseStatisticsVO> statistics(@RequestParam(required = false) Long deptId,
                                                   @RequestParam(required = false) String purchaseType) {
        return Result.success(purchaseService.getStatistics(deptId, purchaseType));
    }

    @GetMapping("/types")
    public Result<List<String>> types() {
        return Result.success(java.util.Arrays.asList("OFFICE", "IT", "EQUIPMENT", "OTHER"));
    }
}
