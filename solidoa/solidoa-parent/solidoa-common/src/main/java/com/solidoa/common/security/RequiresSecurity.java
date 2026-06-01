package com.solidoa.common.security;

import java.lang.annotation.*;

/**
 * 安全校验注解
 * 标记需要验证用户身份的方法
 *
 * 使用示例：
 * ```java
 * @RequiresSecurity
 * public void deleteFile(Long fileId) {
 *     SecurityValidator.requireVerified();
 *     // 业务逻辑
 * }
 * ```
 *
 * @Target(ElementType.METHOD) 只能用于方法
 * @Retention(RetentionPolicy.RUNTIME) 运行时生效
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresSecurity {

    /**
     * 是否要求外部请求（经过网关验证）
     * 默认 false，表示内部服务调用也允许
     */
    boolean requireExternal() default false;

    /**
     * 是否要求用户已验证
     * 默认 true
     */
    boolean requireVerified() default true;
}