package com.guoyu.platform.competition.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "platform.competition.messaging", name = "enabled", havingValue = "true")
public class CompetitionMessagingConfiguration {

  @Bean
  public TopicExchange competitionEventsExchange(CompetitionMessagingProperties properties) {
    return new TopicExchange(properties.getExchange(), true, false);
  }

  @Bean
  public TopicExchange competitionPlayerRealtimeExchange(CompetitionMessagingProperties properties) {
    return new TopicExchange(properties.getPlayerRealtimeExchange(), true, false);
  }

  @Bean
  public DirectExchange competitionDeadLetterExchange(CompetitionMessagingProperties properties) {
    return new DirectExchange(properties.getDeadLetterExchange(), true, false);
  }

  @Bean
  public Queue competitionNoticeDeliveryQueue(CompetitionMessagingProperties properties) {
    return QueueBuilder.durable(properties.getQueue())
        .deadLetterExchange(properties.getDeadLetterExchange())
        .deadLetterRoutingKey(properties.getQueue())
        .build();
  }

  @Bean
  public Queue competitionNoticeDeliveryDeadLetterQueue(CompetitionMessagingProperties properties) {
    return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
  }

  /** Each API node gets its own ephemeral queue so every online-player node receives the event. */
  @Bean
  public Queue competitionPlayerRealtimeQueue() {
    return new AnonymousQueue();
  }

  @Bean
  public Binding competitionNoticeDeliveryBinding(
      Queue competitionNoticeDeliveryQueue,
      TopicExchange competitionEventsExchange,
      CompetitionMessagingProperties properties) {
    return BindingBuilder.bind(competitionNoticeDeliveryQueue)
        .to(competitionEventsExchange)
        .with(properties.getRoutingKey());
  }

  @Bean
  public Binding competitionNoticeDeliveryDeadLetterBinding(
      Queue competitionNoticeDeliveryDeadLetterQueue,
      DirectExchange competitionDeadLetterExchange,
      CompetitionMessagingProperties properties) {
    return BindingBuilder.bind(competitionNoticeDeliveryDeadLetterQueue)
        .to(competitionDeadLetterExchange)
        .with(properties.getQueue());
  }

  @Bean
  public Binding competitionPlayerRealtimeBinding(
      @org.springframework.beans.factory.annotation.Qualifier("competitionPlayerRealtimeQueue")
          Queue competitionPlayerRealtimeQueue,
      @org.springframework.beans.factory.annotation.Qualifier("competitionPlayerRealtimeExchange")
          TopicExchange competitionPlayerRealtimeExchange,
      CompetitionMessagingProperties properties) {
    return BindingBuilder.bind(competitionPlayerRealtimeQueue)
        .to(competitionPlayerRealtimeExchange)
        .with(properties.getPlayerRealtimeRoutingKey());
  }
}
