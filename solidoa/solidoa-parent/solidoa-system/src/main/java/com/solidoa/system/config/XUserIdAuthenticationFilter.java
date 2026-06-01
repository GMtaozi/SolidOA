package com.solidoa.system.config;

import com.solidoa.common.security.UserContext;
import com.solidoa.common.security.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 从请求头 X-User-Id 提取用户身份
 * 用于网关转发请求时的身份认证
 */
public class XUserIdAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");

        if (userId != null && !userId.isEmpty()) {
            try {
                // 设置用户上下文
                UserContext context = new UserContext();
                context.setUserId(Long.parseLong(userId));
                context.setUsername(request.getHeader("X-User-Name"));
                String deptId = request.getHeader("X-User-DeptId");
                if (deptId != null && !deptId.isEmpty()) {
                    context.setDeptId(Long.parseLong(deptId));
                }
                context.setRoles(request.getHeader("X-User-Roles"));
                context.setSource("EXTERNAL");
                context.setVerified(true);
                UserContextHolder.set(context);

                // 设置 Spring Security 权限
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                // 解析 X-User-Roles 请求头并设置对应的权限
                String rolesHeader = request.getHeader("X-User-Roles");
                if (rolesHeader != null && !rolesHeader.isEmpty()) {
                    String[] roles = rolesHeader.split(",");
                    for (String role : roles) {
                        role = role.trim();
                        if (!role.isEmpty()) {
                            // 统一转大写，与 @PreAuthorize("hasAnyRole('ADMIN','HR')") 保持一致
                            String upperRole = role.toUpperCase();
                            // 添加 ROLE_ 前缀的角色
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + upperRole));
                            // 同时添加不带前缀的权限（用于 @PreAuthorize hasAuthority）
                            authorities.add(new SimpleGrantedAuthority(role));
                            authorities.add(new SimpleGrantedAuthority(upperRole));
                        }
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException e) {
                // 忽略无效的 userId
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
