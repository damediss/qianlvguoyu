package com.guoyu.platform.security.jwt;

import com.guoyu.platform.security.PlatformUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private final JwtProperties properties;
  private final SecretKey secretKey;

  public JwtTokenService(JwtProperties properties) {
    this.properties = properties;
    this.secretKey = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(PlatformUserPrincipal principal) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(properties.accessTokenTtlSeconds());
    return Jwts.builder()
        .subject(String.valueOf(principal.getUserId()))
        .claim("tenantId", principal.getTenantId())
        .claim("username", principal.getUsername())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public long getAccessTokenTtlSeconds() {
    return properties.accessTokenTtlSeconds();
  }

  public long getRefreshTokenTtlSeconds() {
    return properties.refreshTokenTtlSeconds();
  }
}
