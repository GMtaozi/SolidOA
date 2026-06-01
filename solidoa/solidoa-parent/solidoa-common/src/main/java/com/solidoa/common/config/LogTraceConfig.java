package com.solidoa.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import com.solidoa.common.constant.TraceConstants;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * 日志追踪配置
 * - 从请求头获取 TraceId
 * - 写入 MDC 供 Logback 使用
 */
@Configuration
public class LogTraceConfig {

    @Bean
    public FilterRegistrationBean<TraceIdServletFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdServletFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Servlet 级别的 TraceId 过滤器
     * 用于非 Gateway 网关的服务（如 system-service 直接被访问时）
     */
    @Component
    public static class TraceIdServletFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String traceId = httpRequest.getHeader(TraceConstants.TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = java.util.UUID.randomUUID().toString().replace("-", "");
            }

            try {
                MDC.put(TraceConstants.TRACE_ID_KEY, traceId);
                chain.doFilter(request, response);
            } finally {
                MDC.remove(TraceConstants.TRACE_ID_KEY);
            }
        }
    }
}
