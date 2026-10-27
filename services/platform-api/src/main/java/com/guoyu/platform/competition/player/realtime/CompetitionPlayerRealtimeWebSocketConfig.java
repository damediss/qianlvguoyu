package com.guoyu.platform.competition.player.realtime;

import com.guoyu.platform.config.WebSocketAllowedOrigins;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CompetitionPlayerRealtimeWebSocketConfig implements WebSocketConfigurer {

  private final CompetitionPlayerRealtimeWebSocketHandler handler;
  private final WebSocketAllowedOrigins allowedOrigins;

  public CompetitionPlayerRealtimeWebSocketConfig(
      CompetitionPlayerRealtimeWebSocketHandler handler,
      WebSocketAllowedOrigins allowedOrigins) {
    this.handler = handler;
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler, "/ws/competition-player")
        .setAllowedOrigins(allowedOrigins.asArray());
  }
}
