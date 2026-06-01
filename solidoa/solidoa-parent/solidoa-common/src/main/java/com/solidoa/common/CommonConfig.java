package com.solidoa.common;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Common 模块配置
 * - 启用 Feign 客户端
 * - 启用重试机制
 */
@Configuration
@EnableFeignClients(basePackages = "com.solidoa.common.client")
@EnableRetry
public class CommonConfig {
}