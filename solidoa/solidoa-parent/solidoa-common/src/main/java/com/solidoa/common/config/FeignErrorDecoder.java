package com.solidoa.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solidoa.common.result.Result;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 错误解码器 - 统一处理下游服务返回的错误
 */
@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper;

    @Autowired
    public FeignErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            String body = readBody(response);
            // 脱敏处理：移除可能的敏感信息
            String sanitizedBody = sanitizeLog(body);
            log.warn("Feign 调用失败: method={}, status={}, body={}", methodKey, response.status(), sanitizedBody);

            // 尝试解析 Result 响应
            if (body != null && body.contains("\"code\":")) {
                try {
                    Result<?> result = objectMapper.readValue(body, Result.class);
                    return new FeignBusinessException(result.getCode(), result.getMessage());
                } catch (Exception ignored) {
                    // 解析失败，使用默认处理
                }
            }

            // 根据 HTTP 状态码生成异常
            return switch (response.status()) {
                case 401 -> new FeignBusinessException(401, "认证失败，请重新登录");
                case 403 -> new FeignBusinessException(403, "无权限访问");
                case 404 -> new FeignBusinessException(404, "资源不存在");
                case 429 -> new FeignBusinessException(429, "请求过于频繁，请稍后重试");
                default -> new FeignBusinessException(500, "服务调用失败");
            };
        } catch (IOException e) {
            log.error("读取 Feign 响应体失败", e);
            return new FeignBusinessException(500, "服务调用失败");
        }
    }

    private static final int MAX_BODY_LENGTH = 1024 * 1024; // 限制最大读取 1MB

    /**
     * 日志脱敏：移除敏感字段防止泄露
     */
    private String sanitizeLog(String body) {
        if (body == null) return null;
        // 脱敏密码、token、银行卡号、手机号等敏感信息
        return body
            .replaceAll("(\"password\"\\s*:\\s*\\\")[^\"]*(\\\")", "$1******$2")
            .replaceAll("(\"token\"\\s*:\\s*\\\")[^\"]*(\\\")", "$1******$2")
            .replaceAll("(\"accessToken\"\\s*:\\s*\\\")[^\"]*(\\\")", "$1******$2")
            .replaceAll("(\"bankAccount\"\\s*:\\s*\\\")[^\"]*(\\\")", "$1******$2")
            .replaceAll("(\"mobile\"\\s*:\\s*\\\")1[3-9]\\d{9}(\\\")", "$1******$2");
    }

    private String readBody(Response response) throws IOException {
        if (response.body() == null) {
            return null;
        }

        // 使用 BufferedReader 流式读取并限制最大长度，避免大响应导致 OOM
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body().asInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            long totalRead = 0;
            while ((read = reader.read(buffer)) != -1) {
                if (totalRead + read > MAX_BODY_LENGTH) {
                    // 超出限制，截断并添加提示
                    sb.append(buffer, 0, (int) (MAX_BODY_LENGTH - totalRead));
                    sb.append("... [truncated, body too large]");
                    break;
                }
                sb.append(buffer, 0, read);
                totalRead += read;
            }
            return sb.toString();
        }
    }

    /**
     * Feign 业务异常
     */
    public static class FeignBusinessException extends RuntimeException {
        private final int code;

        public FeignBusinessException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}