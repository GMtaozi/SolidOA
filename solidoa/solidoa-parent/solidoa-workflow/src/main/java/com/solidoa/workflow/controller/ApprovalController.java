package com.solidoa.workflow.controller;

import com.solidoa.common.result.Result;
import com.solidoa.workflow.service.ApprovalNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审批Controller - 审批流程管理
 */
@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
@Tag(name = "审批管理", description = "审批节点管理")
public class ApprovalController {

    private final ApprovalNodeService nodeService;

    @GetMapping("/tasks/pending/count")
    @Operation(summary = "获取待审批数量")
    public Result<Integer> getPendingCount(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        return Result.success(nodeService.countMyPending(userId));
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("用户ID格式错误");
        }
    }
}
