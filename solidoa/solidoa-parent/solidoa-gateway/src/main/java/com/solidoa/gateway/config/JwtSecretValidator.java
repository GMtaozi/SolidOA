package com.solidoa.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 配置校验器
 * - 启动时检查 JWT Secret 长度
 * - 强制要求 256 位（32字节）以确保 HMAC-SHA256 安全性
 */
@Component
@Slf4j
public class JwtSecretValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final int MIN_SECRET_LENGTH = 32; // 256 位

    @PostConstruct
    public void validateSecret() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT Secret 未配置，请设置 jwt.secret 配置项");
        }

        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "JWT Secret 长度不足 " + MIN_SECRET_LENGTH + " 字符，当前长度: " + jwtSecret.length()
                + "，拒绝启动。请使用至少 32 字符的随机字符串作为 jwt.secret");
        }

        log.info("JWT Secret 配置校验通过，长度: {} 字符", jwtSecret.length());
    }
}