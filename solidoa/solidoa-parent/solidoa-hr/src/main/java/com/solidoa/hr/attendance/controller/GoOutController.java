package com.solidoa.hr.attendance.controller;

import com.solidoa.hr.attendance.form.GoOutForm;
import com.solidoa.hr.attendance.service.GoOutService;
import com.solidoa.hr.attendance.vo.GoOutVO;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 外出申请控制器
 */
@RestController
@RequestMapping("/api/v1/hr/attendance/go-out")
@Slf4j
public class GoOutController {

    @Autowired
    private GoOutService goOutService;

    /**
     * 新建外出申请
     */
    @PostMapping
    public Result<Long> create(@RequestBody @Valid GoOutForm form,
                               @RequestHeader("X-User-Id") Long userId) {
        return Result.success(goOutService.create(form, userId));
    }

    /**
     * 外出列表
     */
    @GetMapping
    public Result<PageVO<GoOutVO>> list(@RequestHeader("X-User-Id") Long userId,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(goOutService.pageList(userId, pageNum, pageSize));
    }

    /**
     * 外出详情
     */
    @GetMapping("/{id}")
    public Result<GoOutVO> getById(@PathVariable Long id,
                                    @RequestHeader("X-User-Id") Long userId) {
        return Result.success(goOutService.getById(id, userId));
    }

    /**
     * 审批外出申请
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                               @RequestBody Map<String, String> body,
                               @RequestHeader("X-User-Id") Long approverId) {
        goOutService.approve(id, body.get("result"), approverId);
        return Result.success();
    }

    /**
     * 撤回外出申请
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        goOutService.cancel(id, userId);
        return Result.success();
    }
}