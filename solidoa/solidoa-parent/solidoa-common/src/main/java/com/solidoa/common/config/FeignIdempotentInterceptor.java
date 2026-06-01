package com.solidoa.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Feign 幂等请求拦截器
 * - 自动添加幂等 Key
 * - 处理 Feign 重试时的幂等问题
 */
@Component
@Slf4j
public class FeignIdempotentInterceptor implements RequestInterceptor {

    private static final String IDEMPOTENT_KEY_HEADER = "X-Idempotent-Key";

    @Override
    public void apply(RequestTemplate template) {
        // 检查请求头是否已有幂等键
        java.util.Collection<String> existingKeys = template.headers().get(IDEMPOTENT_KEY_HEADER);
        String idempotentKey = existingKeys != null && !existingKeys.isEmpty() ? existingKeys.iterator().next() : null;
        if (idempotentKey == null || idempotentKey.isEmpty()) {
            // 从当前请求上下文获取
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                idempotentKey = attributes.getRequest().getHeader(IDEMPOTENT_KEY_HEADER);
            }
        }

        // 如果还没有，生成一个新的
        if (idempotentKey == null || idempotentKey.isEmpty()) {
            idempotentKey = generateIdempotentKey(template);
        }

        // 添加幂等键到请求头
        template.header(IDEMPOTENT_KEY_HEADER, idempotentKey);
        log.debug("Feign 请求添加幂等键: {}", idempotentKey);
    }

    private String generateIdempotentKey(RequestTemplate template) {
        String method = template.method();
        String targetUrl = template.feignTarget().url();
        String path = template.url();
        String fullUrl = targetUrl + path;
        java.util.Map<String, java.util.Collection<String>> queries = template.queries();
        if (queries != null && !queries.isEmpty()) {
            StringBuilder sb = new StringBuilder(fullUrl).append('?');
            queries.forEach((k, v) -> v.forEach(val -> sb.append(k).append('=').append(val).append('&')));
            sb.setLength(sb.length() - 1);
            fullUrl = sb.toString();
        }
        byte[] body = template.body();
        String bodyHash = body != null ? md5Hex(new String(body, StandardCharsets.UTF_8)) : "no-body";
        return String.format("%s-%s-%s", method, fullUrl, bodyHash);
    }

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}