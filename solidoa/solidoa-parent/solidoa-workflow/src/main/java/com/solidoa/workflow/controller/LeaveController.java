package com.solidoa.workflow.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.workflow.form.ApproveForm;
import com.solidoa.workflow.form.LeaveForm;
import com.solidoa.workflow.vo.LeaveVO;
import com.solidoa.workflow.service.LeaveService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 请假申请Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/leave")
@Slf4j
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid LeaveForm form,
                               @RequestHeader("X-User-Id") Long userId) {
        return Result.success(leaveService.createLeave(form, userId));
    }

    @GetMapping
    public Result<PageVO<LeaveVO>> list(PageDTO dto,
                                       @RequestHeader("X-User-Id") Long userId,
                                       @RequestParam(required = false) String status) {
        return Result.success(leaveService.pageList(dto, userId, status));
    }

    @GetMapping("/{id}")
    public Result<LeaveVO> getById(@PathVariable Long id,
                                  @RequestHeader("X-User-Id") Long userId) {
        return Result.success(leaveService.getById(id, userId));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody @Valid ApproveForm form,
                                @RequestHeader("X-User-Id") Long approverId) {
        leaveService.approve(id, form.getApproveType(), form.getComment(), approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        leaveService.cancel(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/add-sign")
    public Result<Void> addSign(@PathVariable Long id,
                                @RequestBody Map<String, Object> body,
                                @RequestHeader("X-User-Id") Long approverId) {
        Object addUserObj = body.get("addUserId");
        if (addUserObj == null) {
            throw new com.solidoa.common.exception.BusinessException(400, "addUserId 不能为空");
        }
        Long addUserId = Long.valueOf(addUserObj.toString());
        leaveService.addSign(id, addUserId, approverId);
        return Result.success();
    }

    @PostMapping("/{id}/transfer")
    public Result<Void> transfer(@PathVariable Long id,
                                 @RequestBody Map<String, Object> body,
                                 @RequestHeader("X-User-Id") Long approverId) {
        Object toUserObj = body.get("toUserId");
        if (toUserObj == null) {
            throw new com.solidoa.common.exception.BusinessException(400, "toUserId 不能为空");
        }
        Long toUserId = Long.valueOf(toUserObj.toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        leaveService.transfer(id, toUserId, reason, approverId);
        return Result.success();
    }

    /**
     * 根据ID获取请假记录（供其他服务 Feign 调用，无权限校验）
     */
    @GetMapping("/simple/{id}")
    public Result<LeaveVO> getByIdForFeign(@PathVariable Long id) {
        LeaveVO leave = leaveService.getByIdSimple(id);
        return Result.success(leave);
    }
}
