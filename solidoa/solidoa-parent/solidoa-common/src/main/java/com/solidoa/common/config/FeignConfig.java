package com.solidoa.common.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Retryer;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign 全局配置
 */
@Configuration
public class FeignConfig {

    /**
     * 连接超时：5秒
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5 * 1000, TimeUnit.MILLISECONDS,  // connectTimeout
            10 * 1000, TimeUnit.MILLISECONDS,  // readTimeout
            true                             // followRedirects
        );
    }

    /**
     * Feign 日志级别：通过配置项 feign.client.config.default.loggerLevel 控制
     * 注意：生产环境应设置为 NONE 避免日志过大
     */
    @Value("${feign.client.config.default.loggerLevel:BASIC}")
    private Logger.Level feignLoggerLevel;

    @Bean
    public Logger.Level feignLoggerLevel() {
        return feignLoggerLevel;
    }

    /**
     * Feign 错误解码器 - 使用 Spring 注入的 Bean
     */
    @Bean
    public ErrorDecoder errorDecoder(FeignErrorDecoder feignErrorDecoder) {
        return feignErrorDecoder;
    }

    /**
     * 重试策略：只对 GET 请求启用重试
     * - 最大重试次数: 1 (即最多执行2次)
     * - 初始重试间隔: 100ms
     * - POST/PUT/DELETE 不重试，防止创建操作意外重试导致数据问题
     */
    @Bean
    public Retryer feignRetryer() {
        return new HttpMethodBasedRetryer();
    }

    /**
     * 基于 HTTP 方法的重试器
     * GET 请求可重试，POST/PUT/DELETE 不重试
     */
    static class HttpMethodBasedRetryer implements Retryer {
        private final int maxAttempts = 1;
        private final long interval = 100;
        private int attempt = 1;

        @Override
        public void continueOrPropagate(RetryableException e) {
            String method = e.request().method();
            // 只对 GET 请求重试
            if (!"GET".equalsIgnoreCase(method)) {
                throw e;
            }
            if (attempt++ >= maxAttempts) {
                throw e;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public Retryer clone() {
            return new HttpMethodBasedRetryer();
        }
    }

    /**
     * Feign 请求拦截器
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 空拦截器，确保请求拦截器链完整
        };
    }
}
