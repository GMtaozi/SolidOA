package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.system.form.LoginForm;
import com.solidoa.system.service.AuthService;
import com.solidoa.system.vo.TokenVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public Result<TokenVO> login(@RequestBody @Valid LoginForm form) {
        return Result.success(authService.login(form));
    }

    @PostMapping("/auth/refresh")
    public Result<TokenVO> refresh(@RequestParam String refreshToken) {
        return Result.success(authService.refreshToken(refreshToken));
    }

    /**
     * 退出登录（幂等设计：缺失token时静默成功，重复调用不会报错）
     */
    @PostMapping("/auth/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            authService.logout(token.substring(7));
        }
        // token为空时直接返回成功，保证幂等性
        return Result.success();
    }
}