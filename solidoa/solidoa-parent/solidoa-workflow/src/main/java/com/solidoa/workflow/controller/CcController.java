package com.solidoa.workflow.controller;

import com.solidoa.common.result.Result;
import com.solidoa.workflow.service.ApprovalCcService;
import com.solidoa.workflow.vo.ApprovalCcVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 抄送Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/cc")
@RequiredArgsConstructor
@Tag(name = "抄送管理", description = "抄送记录管理")
public class CcController {

    private final ApprovalCcService ccService;

    @GetMapping("/my")
    @Operation(summary = "获取我的抄送列表")
    public Result<List<ApprovalCcVO>> getMyCcList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        return Result.success(ccService.getMyCcList(userId, page, size));
    }

    @GetMapping("/my/unread-count")
    @Operation(summary = "获取我的未读抄送数量")
    public Result<Integer> getMyUnreadCount(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        return Result.success(ccService.getMyUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记抄送已读")
    public Result<Void> markAsRead(@PathVariable Long id,
                                    @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        ccService.markAsRead(id, userId);
        return Result.success();
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
