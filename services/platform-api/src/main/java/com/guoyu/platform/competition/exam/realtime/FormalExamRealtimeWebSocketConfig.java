package com.guoyu.platform.competition.exam.realtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableConfigurationProperties(FormalExamRealtimeProperties.class)
public class FormalExamRealtimeWebSocketConfig implements WebSocketConfigurer {

  private final FormalExamPlayerWebSocketHandler playerHandler;
  private final FormalExamConsoleWebSocketHandler consoleHandler;

  public FormalExamRealtimeWebSocketConfig(
      FormalExamPlayerWebSocketHandler playerHandler,
      FormalExamConsoleWebSocketHandler consoleHandler) {
    this.playerHandler = playerHandler;
    this.consoleHandler = consoleHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(playerHandler, "/ws/competition/formal-exam/player")
        .setAllowedOriginPatterns("*");
    registry.addHandler(consoleHandler, "/ws/competition/formal-exam/console")
        .setAllowedOriginPatterns("*");
  }

  @Bean
  @ConditionalOnBean(RedisConnectionFactory.class)
  public RedisMessageListenerContainer formalExamRealtimeRedisListener(
      RedisConnectionFactory connectionFactory) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(playerHandler, new ChannelTopic(FormalExamRealtimeTypes.CHANNEL));
    return container;
  }
}
