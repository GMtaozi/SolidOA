package com.solidoa.common.constant;

/**
 * 追踪常量
 */
public final class TraceConstants {

    private TraceConstants() {}

    /**
     * TraceId HTTP Header 名称
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC 中的 TraceId Key
     */
    public static final String TRACE_ID_KEY = "traceId";
}
