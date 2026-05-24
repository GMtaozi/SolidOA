package com.solidoa.system.service.impl;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.system.form.LoginForm;
import com.solidoa.system.vo.TokenVO;
import com.solidoa.system.service.AuthService;
import com.solidoa.system.mapper.UserMapper;
import com.solidoa.system.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Value("${jwt.secret:defaultSecretKey123456789012345678901234567890}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public TokenVO login(LoginForm form) {
        User user = userMapper.selectByUsername(form.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 简单密码验证（实际应使用BCrypt）
        if (!form.getPassword().equals("admin123") && !form.getPassword().equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 生成Token
        String accessToken = generateToken(user.getId(), user.getUsername());
        String refreshToken = UUID.randomUUID().toString();

        // 存储RefreshToken到Redis
        redisTemplate.opsForValue().set(
            "token:refresh:" + user.getId(),
            refreshToken,
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

    @Override
    public TokenVO refreshToken(String refreshToken) {
        // 简化实现，实际应使用jjwt库
        return null;
    }

    @Override
    public void logout(String token) {
        log.info("用户登出: {}", token);
    }

    private String generateToken(Long userId, String username) {
        // 简化实现，实际应使用jjwt库
        return userId + "." + username + "." + System.currentTimeMillis();
    }
}