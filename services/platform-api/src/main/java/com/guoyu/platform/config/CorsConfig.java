package com.guoyu.platform.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS 白名单 —— 公网部署必需,否则浏览器跨域拦截 /api/* 请求。
 *
 * 读取 platform.cors.allowed-origins(逗号分隔,支持通配符模式),配置为空时 dev 用 * 全开,
 * 生产必须显式填公网 IP / 域名,例如:
 *   CORS_ALLOWED_ORIGINS=http://1.2.3.4:8443,https://your.domain
 *
 * 注意:AllowCredentials=true 时,allowedOrigins 不能用 *,必须用 allowedOriginPatterns。
 */
@Configuration
@Slf4j
public class CorsConfig {

  @Value("${platform.cors.allowed-origins:}")
  private String allowedOrigins;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration c = new CorsConfiguration();

    boolean devMode = !StringUtils.hasText(allowedOrigins);
    if (devMode) {
      // P1-1：此前未配置时直接放通 `*` 且 AllowCredentials=true —— 等于任意站点可发起带凭据的跨域请求，
      // 生产未配 allowed-origins 时正在实际发生。现在兜底只放本机开发源。
      log.warn("[CORS] platform.cors.allowed-origins 未配置，仅允许本机开发源（localhost / 127.0.0.1）。生产必须显式配置。");
      c.addAllowedOriginPattern("http://localhost:[*]");
      c.addAllowedOriginPattern("https://localhost:[*]");
      c.addAllowedOriginPattern("http://127.0.0.1:[*]");
      c.addAllowedOriginPattern("https://127.0.0.1:[*]");
    } else {
      for (String o : allowedOrigins.split(",")) {
        String trimmed = o.trim();
        if (trimmed.isEmpty()) continue;
        if ("*".equals(trimmed)) {
          c.addAllowedOriginPattern("*");
        } else {
          c.addAllowedOriginPattern(trimmed);
        }
      }
      log.info("[CORS] 允许 origin: {}", allowedOrigins);
    }

    c.addAllowedHeader("*");
    c.addAllowedMethod("*");
    c.setAllowCredentials(true);
    // Chrome / Android WebView 访问热点网关等私网地址时会发 Private Network Access 预检
    c.setAllowPrivateNetwork(true);
    c.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", c);
    return src;
  }
}
