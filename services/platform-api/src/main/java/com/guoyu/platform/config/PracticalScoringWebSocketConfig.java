package com.guoyu.platform.config;

import com.guoyu.platform.certification.scoring.PracticalStartWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class PracticalScoringWebSocketConfig implements WebSocketConfigurer {

  private final PracticalStartWebSocketHandler handler;

  public PracticalScoringWebSocketConfig(PracticalStartWebSocketHandler handler) {
    this.handler = handler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler, "/ws/practical-scoring")
        .setAllowedOriginPatterns("*");
  }
}
