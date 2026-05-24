package com.solidoa.common.client;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.LeaveDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "workflow-service", path = "/api/v1/workflow")
public interface WorkflowClient {

    @GetMapping("/leave/{id}")
    Result<LeaveDTO> getLeaveById(@PathVariable("id") Long id);

    @GetMapping("/expense/{id}")
    Result<LeaveDTO> getExpenseById(@PathVariable("id") Long id);
}