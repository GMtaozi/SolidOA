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
}