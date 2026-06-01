package com.solidoa.workflow.controller;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.result.Result;
import com.solidoa.workflow.entity.ApprovalFlowConfig;
import com.solidoa.workflow.form.ApprovalFlowConfigForm;
import com.solidoa.workflow.service.ApprovalFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 审批流程配置Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/flow")
@RequiredArgsConstructor
@Tag(name = "审批流程配置", description = "审批流程配置管理")
public class ApprovalFlowController {

    private final ApprovalFlowService flowService;

    private void requireAdmin(String roles) {
        if (roles == null || !Arrays.asList(roles.split(",")).contains("ADMIN")) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }

    @PostMapping("/config")
    @Operation(summary = "创建审批流程配置")
    public Result<Long> createConfig(@RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("X-User-Roles") String roles,
                                     @Valid @RequestBody ApprovalFlowConfigForm form) {
        requireAdmin(roles);
        return Result.success(flowService.createConfig(form));
    }

    @PutMapping("/config/{id}")
    @Operation(summary = "更新审批流程配置")
    public Result<Void> updateConfig(@RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("X-User-Roles") String roles,
                                     @PathVariable Long id,
                                     @Valid @RequestBody ApprovalFlowConfigForm form) {
        requireAdmin(roles);
        flowService.updateConfig(id, form);
        return Result.success();
    }

    @DeleteMapping("/config/{id}")
    @Operation(summary = "删除审批流程配置")
    public Result<Void> deleteConfig(@RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("X-User-Roles") String roles,
                                     @PathVariable Long id) {
        requireAdmin(roles);
        flowService.deleteConfig(id);
        return Result.success();
    }

    @GetMapping("/config")
    @Operation(summary = "获取审批流程配置列表")
    public Result<List<ApprovalFlowConfig>> listConfig(@RequestHeader("X-User-Id") Long userId,
                                                       @RequestParam String businessType) {
        return Result.success(flowService.listByBusinessType(businessType));
    }

    @PutMapping("/config/{id}/set-default")
    @Operation(summary = "设置默认流程")
    public Result<Void> setDefault(@RequestHeader("X-User-Id") Long userId,
                                   @RequestHeader("X-User-Roles") String roles,
                                   @PathVariable Long id,
                                   @RequestParam String businessType) {
        requireAdmin(roles);
        flowService.setDefault(id, businessType);
        return Result.success();
    }
}
