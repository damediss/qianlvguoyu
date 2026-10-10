package com.guoyu.platform.config;

import com.guoyu.platform.security.PlatformUserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * 给 id_token 注入 email / name 等自定义 claims，
 * 供 WeKnora 解码后按 email 自动匹配/建号。
 */
@Configuration
public class IdTokenClaimsConfig {

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> idTokenClaimsCustomizer() {
    return context -> {
      if ("id_token".equals(context.getTokenType().getValue())) {
        Object principal = context.getPrincipal().getPrincipal();
        if (principal instanceof PlatformUserPrincipal u) {
          String email = u.getEmail();
          String name = u.getDisplayName();
          if (email != null && !email.isBlank()) {
            context.getClaims().claim("email", email)
                .claim("email_verified", true);
          }
          if (name != null) {
            context.getClaims().claim("name", name);
          }
        }
      }
    };
  }
}
