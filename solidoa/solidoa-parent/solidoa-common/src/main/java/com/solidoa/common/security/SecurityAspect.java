package com.solidoa.common.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 安全校验切面
 * 自动处理 @RequiresSecurity 注解的安全校验
 *
 * 注意：当前禁用以避免参数名反射问题
 */
@Aspect
@Component
@Slf4j
public class SecurityAspect {

    @Pointcut("@annotation(com.solidoa.common.security.RequiresSecurity)")
    public void securityMethods() {}

    @Around("securityMethods()")
    public Object aroundSecurityCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresSecurity annotation = signature.getMethod().getAnnotation(RequiresSecurity.class);

        if (annotation.requireExternal()) {
            SecurityValidator.requireExternalRequest();
        }

        if (annotation.requireVerified()) {
            SecurityValidator.requireVerified();
        }

        return joinPoint.proceed();
    }
}
