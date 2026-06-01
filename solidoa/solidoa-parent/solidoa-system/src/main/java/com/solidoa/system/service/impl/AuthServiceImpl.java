package com.solidoa.system.service.impl;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.system.form.LoginForm;
import com.solidoa.system.vo.TokenVO;
import com.solidoa.system.service.AuthService;
import com.solidoa.system.mapper.UserRoleMapper;
import com.solidoa.system.mapper.UserMapper;
import com.solidoa.system.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_DURATION = 900; // 15分钟
    private static final int MIN_SECRET_LENGTH = 32;

    @Autowired
    private UserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @jakarta.annotation.PostConstruct
    public void validateJwtSecret() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("JWT密钥未配置，请设置 jwt.secret 配置项");
        }
        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT密钥长度不足，最少需要 " + MIN_SECRET_LENGTH + " 字符以确保安全性");
        }
        log.info("JWT密钥验证通过");
    }

    @Override
    public TokenVO login(LoginForm form) {
        // 检查登录失败次数
        String failKey = LOGIN_FAIL_PREFIX + form.getUsername();
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        if (failCountStr != null) {
            int failCount = Integer.parseInt(failCountStr);
            if (failCount >= MAX_FAIL_COUNT) {
                throw new BusinessException("登录失败次数过多，请" + (LOCK_DURATION / 60) + "分钟后再试");
            }
        }

        // 验证用户
        User user = userMapper.selectByUsername(form.getUsername());
        if (user == null) {
            recordLoginFail(failKey);
            throw new BusinessException("用户名或密码错误");
        }

        // 使用BCrypt验证密码
        if (!passwordEncoder.matches(form.getPassword(), user.getPassword())) {
            recordLoginFail(failKey);
            throw new BusinessException("用户名或密码错误");
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 清除登录失败记录
        redisTemplate.delete(failKey);

        // 生成JWT Token
        String accessToken = generateToken(user);

        // 生成RefreshToken
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
            "token:refresh:" + user.getId(),
            refreshToken,
            refreshExpiration,
            TimeUnit.SECONDS
        );
        // 设置反向索引，用于 refreshToken() 方法通过 refreshToken 查找用户
        redisTemplate.opsForValue().set(
            "token:refresh:reverse:" + refreshToken,
            user.getId().toString(),
            refreshExpiration,
            TimeUnit.SECONDS
        );

        TokenVO vo = new TokenVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(jwtExpiration);
        vo.setTokenType("Bearer");
        return vo;
    }

    private void recordLoginFail(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, LOCK_DURATION, TimeUnit.SECONDS);
        }
        log.warn("登录失败次数: {}, key: {}", count, key);
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        // 根据refreshToken查找用户
        String userIdKey = "token:refresh:reverse:" + refreshToken;
        String userIdStr = redisTemplate.opsForValue().get(userIdKey);
        if (userIdStr == null) {
            throw new BusinessException("RefreshToken已过期");
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException("用户不存在或已被禁用");
        }

        // 生成新的AccessToken
        String newAccessToken = generateToken(user);

        // 生成新的RefreshToken
        String newRefreshToken = UUID.randomUUID().toString();
        // 删除旧的reverse索引键（实现token轮换，防止旧token继续刷新）
        String oldReverseKey = "token:refresh:reverse:" + refreshToken;
        redisTemplate.delete(oldReverseKey);
        redisTemplate.opsForValue().set(
            "token:refresh:" + user.getId(),
            newRefreshToken,
            refreshExpiration,
            TimeUnit.SECONDS
        );
        // 设置新的reverse索引
        redisTemplate.opsForValue().set(
            "token:refresh:reverse:" + newRefreshToken,
            user.getId().toString(),
            refreshExpiration,
            TimeUnit.SECONDS
        );

        TokenVO vo = new TokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        vo.setExpiresIn(jwtExpiration);
        vo.setTokenType("Bearer");
        return vo;
    }

    @Override
    public void logout(String token) {
        try {
            // 从token中提取用户ID
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String userId = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("userId", String.class);

            if (userId != null) {
                // 删除RefreshToken
                redisTemplate.delete("token:refresh:" + userId);
                log.info("用户登出成功: userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("登出解析token失败: {}", e.getMessage());
        }
    }

    private String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration * 1000);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        // 获取用户角色
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("deptId", user.getDeptId() != null ? user.getDeptId().toString() : null)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}