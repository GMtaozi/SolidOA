package com.solidoa.common.client;

import com.solidoa.common.config.FeignConfig;
import com.solidoa.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(
    name = "hr-service",
    path = "/api/v1/hr",
    configuration = FeignConfig.class
)
public interface HrClient {

    @GetMapping("/attendance/user/{userId}/month/{yearMonth}/summary")
    Result<Map<String, Object>> getAttendanceSummary(@PathVariable("userId") Long userId,
                                                      @PathVariable("yearMonth") String yearMonth);

    @GetMapping("/finance/user/{userId}/month-expense")
    Result<BigDecimal> getMonthlyExpenseAmount(@PathVariable("userId") Long userId,
                                                @RequestParam("yearMonth") String yearMonth);

    @GetMapping("/finance/user/{userId}/year-expense")
    Result<BigDecimal> getYearlyExpenseAmount(@PathVariable("userId") Long userId,
                                               @RequestParam("year") String year);
}