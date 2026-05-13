package com.guoyu.platform.auth.vo;

public record LoginVO(
    String accessToken,
    String refreshToken,
    long expiresIn,
    CurrentUserVO user
) {
}
