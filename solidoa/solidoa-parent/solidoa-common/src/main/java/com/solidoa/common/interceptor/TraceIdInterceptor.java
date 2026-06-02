package com.solidoa.common.interceptor;

import com.solidoa.common.constant.TraceConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 下游服务 TraceId 拦截器 (V2.0 15.2.1)
 * Gateway 已生成 X-Trace-Id, 这里接收并写入 MDC, 日志中即可检索
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TraceConstants.TRACE_ID_HEADER);
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TraceConstants.TRACE_ID_KEY, traceId);
            // 回写响应头, 方便客户端/浏览器调试
            response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(TraceConstants.TRACE_ID_KEY);
    }
}
