package com.guoyu.platform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 修订方案 §2.4（P1-1）：CORS 兜底收口。
 * 覆盖：未配置时仅本机源放行（外域拒绝）/ 显式配置时仅白名单源放行 / 凭证语义保持。
 */
class CorsConfigTest {

  private CorsConfiguration configFor(String allowedOrigins) throws Exception {
    CorsConfig config = new CorsConfig();
    Field field = CorsConfig.class.getDeclaredField("allowedOrigins");
    field.setAccessible(true);
    field.set(config, allowedOrigins);
    CorsConfigurationSource source = config.corsConfigurationSource();
    return source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/anything"));
  }

  @Test
  @DisplayName("★ 未配置 allowed-origins → 仅本机源放行，外域一律拒绝（此前是 * + credentials）")
  void unconfiguredFallsBackToLocalhostOnly() throws Exception {
    CorsConfiguration config = configFor("");

    assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"));
    assertEquals("https://localhost:5173", config.checkOrigin("https://localhost:5173"));
    assertEquals("http://127.0.0.1:8080", config.checkOrigin("http://127.0.0.1:8080"));
    assertNull(config.checkOrigin("https://evil.example.com"));
    assertNull(config.checkOrigin("https://java.app.gjjd168.com"));
    assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
  }

  @Test
  @DisplayName("★ 显式配置 → 仅白名单源放行（含逗号+空格容错）")
  void configuredAllowsOnlyListedOrigins() throws Exception {
    CorsConfiguration config =
        configFor("https://java.app.gjjd168.com, https://app.gjjd168.com");

    assertEquals("https://java.app.gjjd168.com", config.checkOrigin("https://java.app.gjjd168.com"));
    assertEquals("https://app.gjjd168.com", config.checkOrigin("https://app.gjjd168.com"));
    assertNull(config.checkOrigin("http://localhost:5173"));
    assertNull(config.checkOrigin("https://evil.example.com"));
  }
}
