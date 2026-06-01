package com.solidoa.common.metrics;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 监控指标收集器
 *
 * 使用方式：
 * 1. 在业务方法中调用 MetricsCollector.record("api.leave.create", success, costMs)
 * 2. 通过 /actuator/prometheus 端点暴露给 Prometheus 采集
 */
@Component
@Slf4j
public class MetricsCollector {

    // 接口耗时统计（方法名 -> 耗时累加）
    private final Map<String, AtomicLong> methodTotalTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> methodCallCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> methodErrorCount = new ConcurrentHashMap<>();

    // 并发统计
    private final AtomicLong currentRequests = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);

    // 时间窗口统计（每秒请求数）
    private final Map<String, AtomicLong> methodRequestCount = new ConcurrentHashMap<>();
    private long lastResetTime = System.currentTimeMillis();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 记录接口调用
     * @param method 方法名（如 "api.leave.create"）
     * @param success 是否成功
     * @param costMs 耗时（毫秒）
     */
    public void record(String method, boolean success, long costMs) {
        methodTotalTime.computeIfAbsent(method, k -> new AtomicLong()).addAndGet(costMs);
        methodCallCount.computeIfAbsent(method, k -> new AtomicLong()).incrementAndGet();
        if (!success) {
            methodErrorCount.computeIfAbsent(method, k -> new AtomicLong()).incrementAndGet();
        }
        totalRequests.incrementAndGet();
        methodRequestCount.computeIfAbsent(method, k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * 请求进入（增加并发计数）
     */
    public void requestEnter() {
        currentRequests.incrementAndGet();
    }

    /**
     * 请求离开（减少并发计数）
     */
    public void requestLeave() {
        currentRequests.decrementAndGet();
    }

    /**
     * 获取接口统计
     */
    public MethodMetrics getMethodMetrics(String method) {
        MethodMetrics metrics = new MethodMetrics();
        metrics.setMethod(method);
        metrics.setTotalCalls(methodCallCount.getOrDefault(method, new AtomicLong()).get());
        metrics.setTotalErrors(methodErrorCount.getOrDefault(method, new AtomicLong()).get());
        metrics.setTotalTime(methodTotalTime.getOrDefault(method, new AtomicLong()).get());

        long calls = metrics.getTotalCalls();
        if (calls > 0) {
            metrics.setAvgTime(msToMicros(metrics.getTotalTime() / calls));
            metrics.setErrorRate((double) metrics.getTotalErrors() / calls);
        }

        return metrics;
    }

    /**
     * 获取所有接口统计
     */
    public Map<String, MethodMetrics> getAllMetrics() {
        Map<String, MethodMetrics> result = new ConcurrentHashMap<>();
        for (String method : methodCallCount.keySet()) {
            result.put(method, getMethodMetrics(method));
        }
        return result;
    }

    /**
     * 获取当前并发数
     */
    public long getCurrentConcurrency() {
        return currentRequests.get();
    }

    /**
     * 获取总请求数
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * 每分钟输出一次统计日志
     */
    @Scheduled(fixedRate = 60000)
    public void reportMetrics() {
        log.info("===== 系统监控指标 =====");
        log.info("当前并发数: {}", currentRequests.get());
        log.info("总请求数: {}", totalRequests.get());

        for (Map.Entry<String, MethodMetrics> entry : getAllMetrics().entrySet()) {
            MethodMetrics m = entry.getValue();
            if (m.getTotalCalls() > 0) {
                log.info("接口 {}: 调用 {} 次, 错误 {} 次, 平均耗时 {} μs, 错误率 {:.2f}%",
                    entry.getKey(), m.getTotalCalls(), m.getTotalErrors(),
                    m.getAvgTime(), m.getErrorRate() * 100);
            }
        }
        log.info("========================");
    }

    /**
     * 重置统计（每小时，使用写锁保证原子清除）
     */
    @Scheduled(fixedRate = 3600000)
    public void resetMetrics() {
        lock.writeLock().lock();
        try {
            methodTotalTime.clear();
            methodCallCount.clear();
            methodErrorCount.clear();
            methodRequestCount.clear();
            lastResetTime = System.currentTimeMillis();
            log.info("指标统计已重置");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private long msToMicros(long ms) {
        return ms * 1000;
    }

    @Data
    public static class MethodMetrics {
        private String method;
        private long totalCalls;
        private long totalErrors;
        private long totalTime;
        private long avgTime;
        private double errorRate;
    }
}