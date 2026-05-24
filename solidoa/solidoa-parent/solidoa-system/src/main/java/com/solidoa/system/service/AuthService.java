package com.solidoa.system.service;

import com.solidoa.system.form.LoginForm;
import com.solidoa.system.vo.TokenVO;

public interface AuthService {
    TokenVO login(LoginForm form);
    TokenVO refreshToken(String refreshToken);
    void logout(String token);
}