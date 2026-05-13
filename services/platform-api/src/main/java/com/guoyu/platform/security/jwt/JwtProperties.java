package com.guoyu.platform.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.security")
public record JwtProperties(
    String jwtSecret,
    long accessTokenTtlSeconds,
    long refreshTokenTtlSeconds
) {
}
