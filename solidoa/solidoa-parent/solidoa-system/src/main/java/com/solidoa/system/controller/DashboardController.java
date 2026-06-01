package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.security.UserContextHolder;
import com.solidoa.system.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 首页/仪表盘 Controller
 * 提供首页聚合统计数据
 */
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Long userId = UserContextHolder.getUserId();
        return Result.success(dashboardService.getDashboardStats(userId));
    }

    @GetMapping("/messages")
    public Result<Map<String, Object>> getMessages() {
        Long userId = UserContextHolder.getUserId();
        return Result.success(dashboardService.getRecentMessages(userId));
    }

    @GetMapping("/footer-stats")
    public Result<Map<String, Object>> getFooterStats() {
        Long userId = UserContextHolder.getUserId();
        return Result.success(dashboardService.getFooterStats(userId));
    }
}