package com.solidoa.common.config;

import com.solidoa.common.interceptor.TraceIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置 (V2.0 15.2.1)
 * - 注册 TraceIdInterceptor, 下游服务接收 X-Trace-Id
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private TraceIdInterceptor traceIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor)
            .addPathPatterns("/**");
    }
}
