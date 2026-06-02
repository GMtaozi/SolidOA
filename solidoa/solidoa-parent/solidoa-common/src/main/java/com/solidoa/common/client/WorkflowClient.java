package com.solidoa.common.client;

import com.solidoa.common.config.FeignConfig;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.ExpenseDTO;
import com.solidoa.common.vo.LeaveDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "workflow-service",
    path = "/api/v1/workflow",
    configuration = FeignConfig.class
)
public interface WorkflowClient {

    @GetMapping("/leave/{id}")
    Result<LeaveDTO> getLeaveById(@PathVariable("id") Long id);

    @GetMapping("/expense/{id}")
    Result<ExpenseDTO> getExpenseById(@PathVariable("id") Long id);

    @GetMapping("/attendance/{id}")
    Result<LeaveDTO> getAttendanceById(@PathVariable("id") Long id);

    @GetMapping("/leave/simple/{id}")
    Result<LeaveDTO> getLeaveSimple(@PathVariable("id") Long id);

    /**
     * 初始化业务审批节点（Sprint 3.4 新增：供 hr 端 Feign 远程调用）
     * @param businessType REPAIR_CARD / OVERTIME / BUSINESS_TRIP / GO_OUT / EXPENSE
     * @param businessId 业务单据 ID
     * @param applicantId 申请人 ID
     */
    @PostMapping("/approval/nodes/create")
    Result<Void> createApprovalNodes(
            @RequestParam("businessType") String businessType,
            @RequestParam("businessId") Long businessId,
            @RequestParam("applicantId") Long applicantId);
}