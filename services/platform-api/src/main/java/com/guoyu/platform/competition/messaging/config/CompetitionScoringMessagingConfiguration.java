package com.guoyu.platform.competition.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "platform.competition.formal-exam.scoring-messaging",
    name = "enabled", havingValue = "true", matchIfMissing = false)
public class CompetitionScoringMessagingConfiguration {

  @Bean
  public TopicExchange competitionScoringEventsExchange(CompetitionScoringMessagingProperties p) {
    return new TopicExchange(p.getExchange(), true, false);
  }

  @Bean
  public DirectExchange competitionScoringDeadLetterExchange(CompetitionScoringMessagingProperties p) {
    return new DirectExchange(p.getDeadLetterExchange(), true, false);
  }

  @Bean
  public Queue competitionFormalScoringQueue(CompetitionScoringMessagingProperties p) {
    return QueueBuilder.durable(p.getQueue()).deadLetterExchange(p.getDeadLetterExchange())
        .deadLetterRoutingKey(p.getQueue()).build();
  }

  @Bean
  public Queue competitionFormalScoringDeadLetterQueue(CompetitionScoringMessagingProperties p) {
    return QueueBuilder.durable(p.getDeadLetterQueue()).build();
  }

  @Bean
  public Queue competitionScorePipelineQueue(CompetitionScoringMessagingProperties p) {
    return QueueBuilder.durable(p.getPipelineQueue()).deadLetterExchange(p.getDeadLetterExchange())
        .deadLetterRoutingKey(p.getPipelineQueue()).build();
  }

  @Bean
  public Queue competitionScorePipelineDeadLetterQueue(CompetitionScoringMessagingProperties p) {
    return QueueBuilder.durable(p.getPipelineDeadLetterQueue()).build();
  }

  @Bean
  public Binding competitionFormalScoringBinding(
      Queue competitionFormalScoringQueue, TopicExchange competitionScoringEventsExchange,
      CompetitionScoringMessagingProperties p) {
    return BindingBuilder.bind(competitionFormalScoringQueue)
        .to(competitionScoringEventsExchange).with(p.getRoutingKey());
  }

  @Bean
  public Binding competitionScorePipelineBinding(Queue competitionScorePipelineQueue,
      TopicExchange competitionScoringEventsExchange, CompetitionScoringMessagingProperties p) {
    return BindingBuilder.bind(competitionScorePipelineQueue)
        .to(competitionScoringEventsExchange).with(p.getPipelineRoutingKey());
  }

  @Bean
  public Binding competitionFormalScoringDeadLetterBinding(
      Queue competitionFormalScoringDeadLetterQueue,
      DirectExchange competitionScoringDeadLetterExchange,
      CompetitionScoringMessagingProperties p) {
    return BindingBuilder.bind(competitionFormalScoringDeadLetterQueue)
        .to(competitionScoringDeadLetterExchange).with(p.getQueue());
  }

  @Bean
  public Binding competitionScorePipelineDeadLetterBinding(
      Queue competitionScorePipelineDeadLetterQueue,
      DirectExchange competitionScoringDeadLetterExchange,
      CompetitionScoringMessagingProperties p) {
    return BindingBuilder.bind(competitionScorePipelineDeadLetterQueue)
        .to(competitionScoringDeadLetterExchange).with(p.getPipelineQueue());
  }

  @Bean("competitionScoringListenerFactory")
  public SimpleRabbitListenerContainerFactory competitionScoringListenerFactory(
      ConnectionFactory connectionFactory, CompetitionScoringMessagingProperties p) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setConcurrentConsumers(p.getConsumerConcurrency());
    factory.setMaxConcurrentConsumers(p.getConsumerMaxConcurrency());
    factory.setPrefetchCount(p.getPrefetch());
    factory.setDefaultRequeueRejected(false);
    return factory;
  }
}
