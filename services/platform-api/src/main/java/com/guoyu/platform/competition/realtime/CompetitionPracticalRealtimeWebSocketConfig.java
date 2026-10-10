package com.guoyu.platform.competition.realtime;

import com.guoyu.platform.config.WebSocketAllowedOrigins;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CompetitionPracticalRealtimeWebSocketConfig implements WebSocketConfigurer {
  private final CompetitionPracticalRealtimeWebSocketHandler handler;
  private final WebSocketAllowedOrigins allowedOrigins;

  public CompetitionPracticalRealtimeWebSocketConfig(
      CompetitionPracticalRealtimeWebSocketHandler handler,
      WebSocketAllowedOrigins allowedOrigins) {
    this.handler = handler;
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler, "/ws/competition-practical")
        .setAllowedOrigins(allowedOrigins.asArray());
  }
}
