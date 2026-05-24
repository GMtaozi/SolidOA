package com.solidoa.system.vo;

import lombok.Data;

@Data
public class TokenVO {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
}