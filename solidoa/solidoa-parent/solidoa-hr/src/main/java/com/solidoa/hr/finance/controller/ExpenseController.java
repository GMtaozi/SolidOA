package com.solidoa.hr.finance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.hr.finance.form.ExpenseForm;
import com.solidoa.hr.finance.vo.ExpenseVO;
import com.solidoa.hr.finance.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/finance")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping("/expense")
    public Result<Long> create(@RequestBody @Valid ExpenseForm form,
                               @RequestHeader("X-User-Id") Long userId,
                               @RequestHeader(value = "X-Dept-Id", required = false) Long deptId) {
        return Result.success(expenseService.create(form, userId, deptId));
    }

    @GetMapping("/expense")
    public Result<PageVO<ExpenseVO>> list(PageDTO dto,
                                          @RequestHeader("X-User-Id") Long userId,
                                          @RequestParam(required = false) String status) {
        return Result.success(expenseService.pageList(dto, userId, status));
    }

    @GetMapping("/expense/{id}")
    public Result<ExpenseVO> getById(@PathVariable Long id) {
        return Result.success(expenseService.getById(id));
    }

    @GetMapping("/expense/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam(value = "startDate") String startDate,
                                                  @RequestParam(value = "endDate") String endDate) {
        return Result.success(expenseService.getStatistics(startDate, endDate));
    }

    @PostMapping("/expense/{id}/confirm-payment")
    public Result<Void> confirmPayment(@PathVariable Long id,
                                       @RequestHeader("X-User-Id") Long cashierId,
                                       @RequestBody(required = false) Map<String, String> body) {
        String confirmNote = body != null ? body.get("confirmNote") : null;
        expenseService.confirmPayment(id, cashierId, confirmNote);
        return Result.success();
    }

    @GetMapping("/expense/types")
    public Result<Object> getTypes() {
        return Result.success(Arrays.asList(
            java.util.Map.of("value", "TRAVEL", "label", "差旅费"),
            java.util.Map.of("value", "MEAL", "label", "餐饮费"),
            java.util.Map.of("value", "TRANSPORT", "label", "交通费"),
            java.util.Map.of("value", "OFFICE", "label", "办公费"),
            java.util.Map.of("value", "OTHER", "label", "其他")
        ));
    }

    /**
     * 获取用户当月报销金额（供其他服务 Feign 调用）
     */
    @GetMapping("/user/{userId}/month-expense")
    public Result<java.math.BigDecimal> getMonthlyExpenseAmount(@PathVariable Long userId,
                                                                @RequestParam String yearMonth) {
        java.math.BigDecimal amount = expenseService.getMonthlyExpenseAmount(userId, yearMonth);
        return Result.success(amount);
    }

    /**
     * 获取用户年度报销总额（供其他服务 Feign 调用）
     */
    @GetMapping("/user/{userId}/year-expense")
    public Result<java.math.BigDecimal> getYearlyExpenseAmount(@PathVariable Long userId,
                                                               @RequestParam String year) {
        java.math.BigDecimal amount = expenseService.getYearlyExpenseAmount(userId, year);
        return Result.success(amount);
    }

    /**
     * 审批报销
     */
    @PostMapping("/expense/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody Map<String, String> body,
                                @RequestHeader("X-User-Id") Long approverId) {
        expenseService.approve(id, body.get("approveResult"), body.get("comment"), approverId);
        return Result.success();
    }

    /**
     * 撤回报销
     */
    @PutMapping("/expense/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                                @RequestHeader("X-User-Id") Long userId) {
        expenseService.cancel(id, userId);
        return Result.success();
    }
}