package com.solidoa.workflow.controller;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.result.Result;
import com.solidoa.workflow.dto.FlowGraphVO;
import com.solidoa.workflow.entity.TransferRecord;
import com.solidoa.workflow.enums.ApprovalEvent;
import com.solidoa.workflow.enums.ApprovalState;
import com.solidoa.workflow.mapper.TransferMapper;
import com.solidoa.workflow.service.ApprovalNodeService;
import com.solidoa.workflow.service.FlowGraphService;
import com.solidoa.workflow.service.UniversalApprovalService;
import com.solidoa.workflow.statemachine.ApprovalStateMachine;
import com.solidoa.workflow.statemachine.ApprovalStateMachineConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 审批Controller - 审批流程管理（V2.0 重构）
 *
 * 8 端点：
 * 1. GET  /tasks/pending/count          - 待审批数量
 * 2. POST /approval/{type}/{id}/approve  - 同意
 * 3. POST /approval/{type}/{id}/reject   - 拒绝
 * 4. POST /approval/{type}/{id}/withdraw - 撤回
 * 5. POST /approval/{type}/{id}/transfer - 转交
 * 6. POST /approval/{type}/{id}/add-sign  - 加签
 * 7. GET  /approval/{type}/{id}/flow-graph - 流程图
 * 8. GET  /approval/state-machine/info  - 状态机信息（调试用）
 */
@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "审批管理", description = "审批流程统一入口")
public class ApprovalController {

    private final ApprovalNodeService nodeService;
    private final FlowGraphService flowGraphService;
    private final com.solidoa.workflow.service.UniversalApprovalService universalService;
    private final TransferMapper transferMapper;

    /**
     * 1. 待审批数量
     */
    @GetMapping("/tasks/pending/count")
    @Operation(summary = "获取待审批数量")
    public Result<Integer> getPendingCount(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        return Result.success(nodeService.countMyPending(userId));
    }

    /**
     * 2. 同意（带状态机保护）
     */
    @PostMapping("/approval/{type}/{id}/approve")
    @Operation(summary = "审批同意")
    public Result<Void> approve(
            @PathVariable("type") String businessType,
            @PathVariable("id") Long businessId,
            @RequestHeader(value = "X-User-Id") String userIdHeader,
            @RequestBody(required = false) ApproveForm form) {
        Long userId = parseUserId(userIdHeader);
        ApprovalState newState = fireWithStateMachine(businessType, businessId, userId,
                ApprovalEvent.APPROVE, form != null ? form.getComment() : null, true);
        log.info("审批同意: type={}, id={}, newState={}", businessType, businessId, newState);
        return Result.success();
    }

    /**
     * 3. 拒绝
     */
    @PostMapping("/approval/{type}/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<Void> reject(
            @PathVariable("type") String businessType,
            @PathVariable("id") Long businessId,
            @RequestHeader(value = "X-User-Id") String userIdHeader,
            @RequestBody(required = false) ApproveForm form) {
        Long userId = parseUserId(userIdHeader);
        ApprovalState newState = fireWithStateMachine(businessType, businessId, userId,
                ApprovalEvent.REJECT, form != null ? form.getComment() : null, true);
        log.info("审批拒绝: type={}, id={}, newState={}", businessType, businessId, newState);
        return Result.success();
    }

    /**
     * 4. 撤回（申请人主动）
     */
    @PostMapping("/approval/{type}/{id}/withdraw")
    @Operation(summary = "撤回申请")
    public Result<Void> withdraw(
            @PathVariable("type") String businessType,
            @PathVariable("id") Long businessId,
            @RequestHeader(value = "X-User-Id") String userIdHeader,
            @RequestBody(required = false) ApproveForm form) {
        Long userId = parseUserId(userIdHeader);
        ApprovalState newState = fireWithStateMachine(businessType, businessId, userId,
                ApprovalEvent.WITHDRAW, form != null ? form.getComment() : null, false);
        // 撤回时直接改业务表 status（A1：调 Service）
        universalService.updateBusinessStatus(businessType, businessId, "WITHDRAWN");
        log.info("撤回: type={}, id={}, newState={}", businessType, businessId, newState);
        return Result.success();
    }

    /**
     * 5. 转交
     */
    @PostMapping("/approval/{type}/{id}/transfer")
    @Operation(summary = "转交审批人")
    public Result<Void> transfer(
            @PathVariable("type") String businessType,
            @PathVariable("id") Long businessId,
            @RequestHeader(value = "X-User-Id") String userIdHeader,
            @RequestBody TransferForm form) {
        Long userId = parseUserId(userIdHeader);
        ApprovalState newState = fireWithStateMachine(businessType, businessId, userId,
                ApprovalEvent.TRANSFER, form != null ? form.getReason() : null, true);
        // 转交：更新业务表 currentApproverId（A1：调 Service）
        if (form != null && form.getToUserId() != null) {
            universalService.updateBusinessApprover(businessType, businessId, form.getToUserId(), form.getToUserName());

            // D3：写 oa_transfer_record 表
            TransferRecord record = new TransferRecord();
            record.setBusinessType(businessType);
            record.setBusinessId(businessId);
            record.setFromApproverId(userId);
            record.setToApproverId(form.getToUserId());
            record.setReason(form.getReason());
            record.setCreateTime(java.time.LocalDateTime.now());
            transferMapper.insert(record);
            log.info("转交记录已写入: fromUser={}, toUser={}, type={}, id={}",
                    userId, form.getToUserId(), businessType, businessId);
        }
        log.info("转交: type={}, id={}, newState={}, toUser={}", businessType, businessId, newState, form.getToUserId());
        return Result.success();
    }

    /**
     * 6. 加签
     */
    @PostMapping("/approval/{type}/{id}/add-sign")
    @Operation(summary = "加签")
    public Result<Void> addSign(
            @PathVariable("type") String businessType,
            @PathVariable("id") Long businessId,
            @RequestHeader(value = "X-User-Id") String userIdHeader,
            @RequestBody AddSignForm form) {
        Long userId = parseUserId(userIdHeader);
        ApprovalState newState = fireWithStateMachine(businessType, businessId, userId,
                ApprovalEvent.ADD_SIGN, form != null ? form.getComment() : null, true);
        log.info("加签: type={}, id={}, newState={}", businessType, businessId, newState);
        return Result.success();
    }

    /**
     * 7. 流程图（V2.0 核心：含 edges 真实拓扑）
     */
    @GetMapping("/approval/{type}/{id}/flow-graph")
    @Operation(summary = "获取流程图（节点+边）")
    public Result<FlowGraphVO> getFlowGraph(
            @PathVariable("type") String businessType,
            @PathVariable("id") Long businessId) {
        return Result.success(flowGraphService.getFlowGraph(businessType, businessId));
    }

    /**
     * 8. 状态机信息（调试/文档用）
     */
    @GetMapping("/approval/state-machine/info")
    @Operation(summary = "获取状态机定义")
    public Result<Object> getStateMachineInfo() {
        ApprovalStateMachine sm = ApprovalStateMachineConfig.getInstance();
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("states", java.util.Arrays.stream(ApprovalState.values()).map(Enum::name).toList());
        info.put("events", java.util.Arrays.stream(ApprovalEvent.values()).map(Enum::name).toList());
        return Result.success(info);
    }

    /**
     * 8.5 时间线 (V2.0 6.1.2) - 审批历史 + 当前节点, 供前端 OaApprovalFlow / 时间轴组件
     * 与 flow-graph 的区别: timeline 是顺序时间线, flow-graph 是节点拓扑图
     */
    @GetMapping("/approval/{type}/{id}/timeline")
    @Operation(summary = "获取审批时间线（审批历史 + 当前节点）")
    public Result<java.util.Map<String, Object>> getTimeline(
            @PathVariable("type") String businessType,
            @PathVariable("id") Long businessId) {
        FlowGraphVO flow = flowGraphService.getFlowGraph(businessType, businessId);
        java.util.Map<String, Object> timeline = new java.util.LinkedHashMap<>();
        timeline.put("businessType", businessType);
        timeline.put("businessId", businessId);
        timeline.put("currentState", flow.getCurrentState());
        timeline.put("currentNodeOrder", flow.getCurrentNodeOrder());
        // nodes 已按 order 排序, 直接作为时间线使用
        timeline.put("items", flow.getNodes());
        timeline.put("totalNodes", flow.getTotalNodes());
        timeline.put("completedNodes", flow.getCompletedNodes());
        return Result.success(timeline);
    }

    /**
     * 9. 初始化业务审批节点（Sprint 3.4 修复：供 hr 端 Feign 远程调用）
     * hr 端业务（OVERTIME/BUSINESS_TRIP/REPAIR_CARD/GO_OUT/EXPENSE）创建后
     * 调此端点把节点写一份到 oa_workflow.oa_approval_node
     */
    @PostMapping("/approval/nodes/create")
    @Operation(summary = "初始化业务审批节点（远程给 hr 端调用）")
    public Result<Void> createNodes(
            @RequestParam("businessType") String businessType,
            @RequestParam("businessId") Long businessId,
            @RequestParam("applicantId") Long applicantId) {
        nodeService.createNodes(businessType, businessId, applicantId);
        log.info("远程创建审批节点: type={}, businessId={}, applicantId={}", businessType, businessId, applicantId);
        return Result.success();
    }

    // ========== 私有方法 ==========

    /**
     * 通过状态机触发事件（统一保护）
     */
    private ApprovalState fireWithStateMachine(String businessType, Long businessId,
                                                Long userId, ApprovalEvent event,
                                                String comment, boolean isCurrentApprover) {
        // A1: 委托给 UniversalApprovalService 统一走状态机
        return universalService.fire(businessType, businessId, userId, event, comment);
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

    // ========== Form DTOs ==========

    @lombok.Data
    public static class ApproveForm {
        private String result;     // APPROVE / REJECT
        private String comment;
    }

    @lombok.Data
    public static class TransferForm {
        private Long toUserId;
        private String toUserName;
        private String reason;
    }

    @lombok.Data
    public static class AddSignForm {
        private java.util.List<Long> userIds;
        private String comment;
    }
}
