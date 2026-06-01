package com.solidoa.common.metrics;

import com.solidoa.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控指标接口
 * 提供 HTTP 接口访问系统指标
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    @Autowired
    private MetricsCollector metricsCollector;

    /**
     * 获取系统概览指标
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("currentConcurrency", metricsCollector.getCurrentConcurrency());
        metrics.put("totalRequests", metricsCollector.getTotalRequests());
        metrics.put("methodCount", metricsCollector.getAllMetrics().size());
        return Result.success(metrics);
    }

    /**
     * 获取接口统计列表
     */
    @GetMapping("/methods")
    public Result<Map<String, MetricsCollector.MethodMetrics>> getMethodMetrics() {
        return Result.success(metricsCollector.getAllMetrics());
    }

    /**
     * 获取指定接口统计
     */
    @GetMapping("/method/{name}")
    public Result<MetricsCollector.MethodMetrics> getMethodMetricByName(@PathVariable("name") String name) {
        return Result.success(metricsCollector.getMethodMetrics(name));
    }

    /**
     * Prometheus 格式输出（可被 Prometheus 采集）
     */
    @GetMapping("/prometheus")
    public Result<String> prometheusFormat() {
        StringBuilder sb = new StringBuilder();

        // 并发数
        sb.append("# HELP solidoa_concurrent_requests 当前并发请求数\n");
        sb.append("# TYPE solidoa_concurrent_requests gauge\n");
        sb.append("solidoa_concurrent_requests ").append(metricsCollector.getCurrentConcurrency()).append("\n\n");

        // 总请求数
        sb.append("# HELP solidoa_total_requests 总请求数\n");
        sb.append("# TYPE solidoa_total_requests counter\n");
        sb.append("solidoa_total_requests ").append(metricsCollector.getTotalRequests()).append("\n\n");

        // 各接口统计
        for (Map.Entry<String, MetricsCollector.MethodMetrics> entry : metricsCollector.getAllMetrics().entrySet()) {
            String name = entry.getKey().replace(".", "_").replace("-", "_");
            MetricsCollector.MethodMetrics m = entry.getValue();

            sb.append("# HELP solidoa_method_calls ").append(entry.getKey()).append(" 调用次数\n");
            sb.append("# TYPE solidoa_method_calls counter\n");
            sb.append("solidoa_method_calls{method=\"").append(entry.getKey()).append("\"} ")
                .append(m.getTotalCalls()).append("\n");

            sb.append("# HELP solidoa_method_errors ").append(entry.getKey()).append(" 错误次数\n");
            sb.append("# TYPE solidoa_method_errors counter\n");
            sb.append("solidoa_method_errors{method=\"").append(entry.getKey()).append("\"} ")
                .append(m.getTotalErrors()).append("\n");

            sb.append("# HELP solidoa_method_avg_time ").append(entry.getKey()).append(" 平均耗时(微秒)\n");
            sb.append("# TYPE solidoa_method_avg_time gauge\n");
            sb.append("solidoa_method_avg_time{method=\"").append(entry.getKey()).append("\"} ")
                .append(m.getAvgTime()).append("\n\n");
        }

        return Result.success(sb.toString());
    }
}