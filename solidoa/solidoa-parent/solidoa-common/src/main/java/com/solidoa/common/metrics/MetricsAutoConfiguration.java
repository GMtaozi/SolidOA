package com.solidoa.common.metrics;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 指标配置
 * MetricsAspect 已禁用，当前仅保留 MetricsCollector 和 MetricsController
 */
@Configuration
@Profile("!dev")
public class MetricsAutoConfiguration {
    // 禁用 MetricsAspect 以避免参数名反射问题
}
