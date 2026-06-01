package com.solidoa.common.metrics;

import java.lang.annotation.*;

/**
 * 方法耗时监控注解
 *
 * 使用示例：
 * ```java
 * @Timed(value = "api.leave.create", slowThreshold = 2000)
 * public Long createLeave(LeaveForm form) {
 *     // 业务逻辑
 * }
 * ```
 *
 * @author solid-oa
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {

    /**
     * 指标名称（用于 Prometheus 等监控工具）
     * 默认为空，自动使用 "类名.方法名"
     */
    String value() default "";

    /**
     * 慢请求阈值（毫秒）
     * 超过此阈值的方法调用会被记录为慢请求
     * 默认 1 秒
     */
    long slowThreshold() default 1000;
}