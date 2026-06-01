package com.solidoa.hr.finance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.hr.finance.form.BudgetForm;
import com.solidoa.hr.finance.service.BudgetService;
import com.solidoa.hr.finance.vo.BudgetVO;
import com.solidoa.hr.finance.vo.BudgetWarningVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/finance/budget")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @GetMapping
    public Result<List<BudgetVO>> list(@RequestParam(required = false) Integer year,
                                       @RequestParam(required = false) Integer month) {
        return Result.success(budgetService.list(year, month));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid BudgetForm form) {
        return Result.success(budgetService.create(form));
    }

    @PutMapping
    public Result<Void> update(@RequestBody @Valid BudgetForm form) {
        budgetService.update(form);
        return Result.success();
    }

    @GetMapping("/warnings")
    public Result<List<BudgetWarningVO>> getWarnings() {
        return Result.success(budgetService.getWarnings());
    }

    @PostMapping("/adjust")
    public Result<Void> adjust(@RequestBody Map<String, Object> body) {
        Long budgetId = ((Number) body.get("budgetId")).longValue();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String adjustType = (String) body.get("adjustType");
        String reason = (String) body.get("reason");
        budgetService.adjust(budgetId, amount, adjustType, reason);
        return Result.success();
    }
}