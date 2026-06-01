package com.solidoa.system.vo;

import lombok.Data;

@Data
public class TokenVO {
    private String accessToken;
    private String refreshToken;
    /** 剩余有效秒数，符合OAuth2标准 */
    private Long expiresIn;
    private String tokenType;
}