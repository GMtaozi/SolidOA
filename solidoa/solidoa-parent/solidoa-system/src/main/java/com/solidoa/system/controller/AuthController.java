package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.system.form.LoginForm;
import com.solidoa.system.service.AuthService;
import com.solidoa.system.vo.TokenVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/auth/login")
    public Result<TokenVO> login(@RequestBody @Valid LoginForm form) {
        return Result.success(authService.login(form));
    }

    @PostMapping("/auth/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            authService.logout(token.substring(7));
        }
        return Result.success();
    }
}