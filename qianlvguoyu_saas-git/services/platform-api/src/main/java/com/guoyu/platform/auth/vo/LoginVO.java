package com.guoyu.platform.auth.vo;

public record LoginVO(
    String accessToken,
    String refreshToken,
    long expiresIn,
    Long tenantId,
    CurrentUserVO user
) {
}
