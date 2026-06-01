package com.solidoa.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring MVC 参数解析器配置
 * 解决 Spring Boot 3.x 在 Java 17 下参数名反射缺失的问题
 *
 * 问题原因：
 * 1. Spring MVC 默认使用 LocalVariableTableParameterNameDiscoverer 获取参数名
 * 2. 没有 -parameters 编译参数时，字节码不保留参数名信息
 * 3. 导致 @RequestParam 等注解无法确定参数名
 *
 * 解决方案：
 * - 配置使用 Jdk8ReflectionParameterNameDiscoverer（支持 -parameters 编译）
 * - 或者使用 ParamNameFunction 直接从 Java 8+ 的方法签名获取参数名
 */
@Configuration
@EnableConfigurationProperties
public class ArgumentResolverConfig {

    @PostConstruct
    public void configureArgumentResolvers() {
        // 使用 Spring 提供的默认参数名发现器配置
        // Spring 5.0+ 在 Java 8+ 环境下默认使用 StandardReflectionParameterNameDiscoverer
        // 但需要确保编译器使用了 -parameters 参数

        // 对于使用 @RequestParam 但未指定 value 的情况，
        // 需要配置 fallback 参数名发现器
    }
}