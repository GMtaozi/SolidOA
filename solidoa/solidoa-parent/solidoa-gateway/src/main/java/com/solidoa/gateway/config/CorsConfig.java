package com.solidoa.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    // 限制允许的请求头，只包含实际需要的请求头
    private static final java.util.Set<String> ALLOWED_HEADERS = java.util.Set.of(
        "Content-Type",
        "Authorization",
        "X-Requested-With",
        "X-User-Id",
        "X-Request-Id",
        "Accept",
        "Accept-Language",
        "Origin",
        "Cache-Control"
    );

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        boolean hasSpecificOrigins = allowedOrigins != null && !allowedOrigins.isBlank();

        if (!hasSpecificOrigins) {
            // 无配置时使用通配符，但不允许凭证（安全默认）
            config.addAllowedOriginPattern("*");
            config.setAllowCredentials(false);
        } else {
            // 有具体域名配置时，支持凭证并使用具体域名
            Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(config::addAllowedOriginPattern);
            config.setAllowCredentials(true);
        }

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 限制允许的请求头，不使用 "*"
        config.setAllowedHeaders(new java.util.ArrayList<>(ALLOWED_HEADERS));
        // 不允许暴露的响应头（安全考虑）
        config.setExposedHeaders(Arrays.asList("X-Request-Id", "X-Total-Count"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}