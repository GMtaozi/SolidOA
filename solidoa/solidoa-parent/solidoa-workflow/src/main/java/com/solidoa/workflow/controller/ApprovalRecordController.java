package com.solidoa.workflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.result.Result;
import com.solidoa.workflow.service.ApprovalCcService;
import com.solidoa.workflow.service.ApprovalRecordService;
import com.solidoa.workflow.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 审批记录Controller
 */
@RestController
@RequestMapping("/api/v1/workflow/record")
public class ApprovalRecordController {

    @Autowired
    private ApprovalRecordService recordService;

    @Autowired
    private ApprovalCcService ccService;

    /**
     * 获取当前用户ID
     */
    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new BusinessException(400, "用户未登录");
        }
        return Long.parseLong(userIdStr);
    }

    /**
     * 我发起的申请
     */
    @GetMapping("/my-apply")
    public Result<IPage<ApprovalRecordVO>> getMyApply(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return Result.success(recordService.getMyApply(pageNum, pageSize, businessType, status,
            startDate, endDate, userId));
    }

    /**
     * 我审批过的申请
     */
    @GetMapping("/my-approved")
    public Result<IPage<ApprovalRecordVO>> getMyApproved(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return Result.success(recordService.getMyApproved(pageNum, pageSize, businessType, status,
            startDate, endDate, userId));
    }

    /**
     * 全部审批记录(管理员)
     */
    @GetMapping("/all")
    public Result<IPage<ApprovalRecordVO>> getAllRecord(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long deptId) {
        if (roles == null || !java.util.Arrays.asList(roles.split(",")).contains("ADMIN")) {
            throw new BusinessException(403, "需要管理员权限");
        }
        return Result.success(recordService.getAllRecord(pageNum, pageSize, businessType, status,
            startDate, endDate, userId, deptId));
    }

    /**
     * 审批记录详情
     */
    @GetMapping("/{id}")
    public Result<ApprovalRecordDetailVO> getRecordDetail(@PathVariable Long id) {
        return Result.success(recordService.getRecordDetail(id));
    }

    /**
     * 审批统计
     */
    @GetMapping("/statistics")
    public Result<ApprovalStatisticsVO> getStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return Result.success(recordService.getStatistics(startDate, endDate, deptId, userId));
    }

    /**
     * 导出审批记录
     */
    @GetMapping("/export")
    public void exportRecord(
            @RequestParam String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) {
        recordService.exportRecord(businessType, status, startDate, endDate, deptId, response);
    }

    /**
     * 我的抄送列表
     */
    @GetMapping("/cc/my")
    public Result<List<ApprovalCcVO>> getMyCc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Boolean isRead,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return Result.success(ccService.getMyCcList(userId, pageNum, pageSize));
    }

    /**
     * 标记抄送已读
     */
    @PutMapping("/cc/{id}/read")
    public Result<Void> markCcRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        ccService.markAsRead(id, userId);
        return Result.success();
    }
}