package com.solidoa.hr.finance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.hr.finance.form.SalaryForm;
import com.solidoa.hr.finance.form.BatchSalaryForm;
import com.solidoa.hr.finance.form.BatchPayForm;
import com.solidoa.hr.finance.service.SalaryService;
import com.solidoa.hr.finance.vo.SalaryVO;
import com.solidoa.hr.finance.vo.SalaryItemVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/finance/salary")
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    @GetMapping("/items")
    public Result<List<SalaryItemVO>> getSalaryItems() {
        return Result.success(salaryService.getSalaryItems());
    }

    @PostMapping
    public Result<Long> createSalary(@RequestBody SalaryForm form,
                                     @RequestHeader("X-User-Id") Long creatorId) {
        return Result.success(salaryService.createSalary(form, creatorId));
    }

    @PostMapping("/batch")
    public Result<Integer> batchCreateSalary(@RequestBody BatchSalaryForm form,
                                             @RequestHeader("X-User-Id") Long creatorId) {
        return Result.success(salaryService.batchCreateSalary(form, creatorId));
    }

    @GetMapping
    public Result<PageVO<SalaryVO>> listSalary(PageDTO dto,
                                               @RequestParam(required = false) String yearMonth,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long deptId) {
        return Result.success(salaryService.listSalary(dto, yearMonth, status, deptId));
    }

    @GetMapping("/{id}")
    public Result<SalaryVO> getSalaryById(@PathVariable Long id) {
        return Result.success(salaryService.getSalaryById(id));
    }

    @PutMapping("/{id}")
    public Result<Void> updateSalary(@PathVariable Long id,
                                     @RequestBody SalaryForm form,
                                     @RequestHeader("X-User-Id") Long updaterId) {
        salaryService.updateSalary(id, form, updaterId);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submitSalary(@PathVariable Long id,
                                     @RequestHeader("X-User-Id") Long submitterId) {
        salaryService.submitSalary(id, submitterId);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelSalary(@PathVariable Long id,
                                     @RequestHeader("X-User-Id") Long userId) {
        salaryService.cancelSalary(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approveSalary(@PathVariable Long id,
                                      @RequestBody(required = false) java.util.Map<String, String> body,
                                      @RequestHeader("X-User-Id") Long approverId) {
        String comment = body != null ? body.get("comment") : null;
        salaryService.approveSalary(id, comment, approverId);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> rejectSalary(@PathVariable Long id,
                                      @RequestBody(required = false) java.util.Map<String, String> body,
                                      @RequestHeader("X-User-Id") Long approverId) {
        String comment = body != null ? body.get("comment") : null;
        salaryService.rejectSalary(id, comment, approverId);
        return Result.success();
    }

    @PostMapping("/{id}/pay")
    public Result<Void> paySalary(@PathVariable Long id,
                                  @RequestHeader("X-User-Id") Long operatorId) {
        salaryService.paySalary(id, operatorId);
        return Result.success();
    }

    @PostMapping("/batch-pay")
    public Result<Integer> batchPaySalary(@RequestBody BatchPayForm form,
                                          @RequestHeader("X-User-Id") Long operatorId) {
        return Result.success(salaryService.batchPaySalary(form, operatorId));
    }

    @GetMapping("/my")
    public Result<List<SalaryVO>> getMySalary(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(salaryService.getMySalary(userId, startMonth, endMonth));
    }

    @GetMapping("/export")
    public void exportSalary(@RequestParam String yearMonth,
                             @RequestParam(required = false) Long deptId,
                             HttpServletResponse response) {
        salaryService.exportSalary(yearMonth, deptId, response);
    }
}