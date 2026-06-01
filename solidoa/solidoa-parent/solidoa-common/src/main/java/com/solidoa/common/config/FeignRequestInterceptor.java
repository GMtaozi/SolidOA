package com.solidoa.common.config;

import com.solidoa.common.constant.TraceConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器 - 安全地传递认证信息和追踪ID到下游服务
 *
 * 安全策略：
 * - 用户身份头(X-User-Id等)仅在请求来自网关时才转发
 * - 内部服务间调用不转发用户身份头，下游服务通过Token验证重新获取身份
 * - 仅转发必要的追踪信息用于链路追踪
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_NAME_HEADER = "X-User-Name";
    private static final String USER_DEPT_HEADER = "X-User-DeptId";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String REQUEST_SOURCE_HEADER = "X-Request-Source";
    private static final String SOURCE_EXTERNAL = "EXTERNAL";

    @Override
    public void apply(RequestTemplate template) {
        // 从当前请求上下文获取 header
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();

        // 安全策略：只有来自网关的外部请求才转发用户身份头
        // 内部服务间调用不应携带用户身份，下游服务需通过Token验证获取身份
        String requestSource = request.getHeader(REQUEST_SOURCE_HEADER);
        boolean isExternalRequest = SOURCE_EXTERNAL.equals(requestSource);

        if (isExternalRequest) {
            // 来自网关的请求，转发用户身份头
            String userId = request.getHeader(USER_ID_HEADER);
            if (userId != null && !userId.isEmpty()) {
                template.header(USER_ID_HEADER, userId);
            }

            String userName = request.getHeader(USER_NAME_HEADER);
            if (userName != null && !userName.isEmpty()) {
                template.header(USER_NAME_HEADER, userName);
            }

            String deptId = request.getHeader(USER_DEPT_HEADER);
            if (deptId != null && !deptId.isEmpty()) {
                template.header(USER_DEPT_HEADER, deptId);
            }

            String roles = request.getHeader(USER_ROLES_HEADER);
            if (roles != null && !roles.isEmpty()) {
                template.header(USER_ROLES_HEADER, roles);
            }
        }
        // 注意：内部服务调用时不会转发用户身份头，防止身份伪造

        // 传递 Authorization（Bearer Token）- 用于下游服务验证用户身份
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && !authHeader.isEmpty()) {
            template.header(AUTHORIZATION_HEADER, authHeader);
        }

        // 传递 TraceId 用于链路追踪（不涉及身份信息，可以安全转发）
        String traceId = MDC.get(TraceConstants.TRACE_ID_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TraceConstants.TRACE_ID_HEADER, traceId);
        }
    }
}