package com.guoyu.platform.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * WebSocket 握手允许的来源（P1-5 / CSWSH）。
 *
 * <p>此前三处 WebSocketConfig 一律 `setAllowedOriginPatterns("*")`，意味着任意站点都能在受害者
 * 浏览器里建立到本服务的 WebSocket 连接。现改为读配置 `platform.websocket.allowed-origins`
 * （逗号分隔），默认只放生产域名与本机开发源。
 */
@Component
public class WebSocketAllowedOrigins {

  private final List<String> origins;

  public WebSocketAllowedOrigins(
      @Value("${platform.websocket.allowed-origins:https://java.app.gjjd168.com,http://localhost:5173,http://localhost:5174,http://localhost:8080,http://127.0.0.1:5173,http://127.0.0.1:5174,http://127.0.0.1:8080}")
      String raw) {
    this.origins = Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  public String[] asArray() {
    return origins.toArray(new String[0]);
  }
}
