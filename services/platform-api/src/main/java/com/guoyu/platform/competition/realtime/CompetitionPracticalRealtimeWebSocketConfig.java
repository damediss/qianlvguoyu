package com.guoyu.platform.competition.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CompetitionPracticalRealtimeWebSocketConfig implements WebSocketConfigurer {
  private final CompetitionPracticalRealtimeWebSocketHandler handler;

  public CompetitionPracticalRealtimeWebSocketConfig(
      CompetitionPracticalRealtimeWebSocketHandler handler) {
    this.handler = handler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler, "/ws/competition-practical").setAllowedOriginPatterns("*");
  }
}
