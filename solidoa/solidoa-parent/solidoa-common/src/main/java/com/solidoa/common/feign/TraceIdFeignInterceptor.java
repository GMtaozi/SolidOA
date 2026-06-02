package com.solidoa.common.feign;

import com.solidoa.common.constant.TraceConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Feign TraceId 透传 (V2.0 15.2.1)
 * 调下游服务时自动把 MDC 中的 traceId 加到请求头
 */
@Component
public class TraceIdFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(TraceConstants.TRACE_ID_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TraceConstants.TRACE_ID_HEADER, traceId);
        }
    }
}
