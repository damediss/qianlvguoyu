package com.guoyu.platform.competition.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.competition.formal-exam.scoring-messaging")
public class CompetitionScoringMessagingProperties {
  private boolean enabled = true;
  private long dispatchDelayMillis = 100L;
  private int batchSize = 100;
  private int maxAttempts = 3;
  private int leaseSeconds = 60;
  private int consumerConcurrency = 2;
  private int consumerMaxConcurrency = 4;
  private int prefetch = 1;
  private String exchange = "competition.events";
  private String routingKey = "competition.formal-score.requested.v1";
  private String queue = "competition.formal-score";
  private String deadLetterExchange = "competition.dlx";
  private String deadLetterQueue = "competition.formal-score.dlq";
  private String pipelineRoutingKey = "competition.score-source.confirmed.v1";
  private String pipelineQueue = "competition.score-pipeline";
  private String pipelineDeadLetterQueue = "competition.score-pipeline.dlq";

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean value) { enabled = value; }
  public long getDispatchDelayMillis() { return dispatchDelayMillis; }
  public void setDispatchDelayMillis(long value) { dispatchDelayMillis = value; }
  public int getBatchSize() { return batchSize; }
  public void setBatchSize(int value) { batchSize = value; }
  public int getMaxAttempts() { return maxAttempts; }
  public void setMaxAttempts(int value) { maxAttempts = value; }
  public int getLeaseSeconds() { return leaseSeconds; }
  public void setLeaseSeconds(int value) { leaseSeconds = value; }
  public int getConsumerConcurrency() { return consumerConcurrency; }
  public void setConsumerConcurrency(int value) { consumerConcurrency = value; }
  public int getConsumerMaxConcurrency() { return consumerMaxConcurrency; }
  public void setConsumerMaxConcurrency(int value) { consumerMaxConcurrency = value; }
  public int getPrefetch() { return prefetch; }
  public void setPrefetch(int value) { prefetch = value; }
  public String getExchange() { return exchange; }
  public void setExchange(String value) { exchange = value; }
  public String getRoutingKey() { return routingKey; }
  public void setRoutingKey(String value) { routingKey = value; }
  public String getQueue() { return queue; }
  public void setQueue(String value) { queue = value; }
  public String getDeadLetterExchange() { return deadLetterExchange; }
  public void setDeadLetterExchange(String value) { deadLetterExchange = value; }
  public String getDeadLetterQueue() { return deadLetterQueue; }
  public void setDeadLetterQueue(String value) { deadLetterQueue = value; }
  public String getPipelineRoutingKey() { return pipelineRoutingKey; }
  public void setPipelineRoutingKey(String value) { pipelineRoutingKey = value; }
  public String getPipelineQueue() { return pipelineQueue; }
  public void setPipelineQueue(String value) { pipelineQueue = value; }
  public String getPipelineDeadLetterQueue() { return pipelineDeadLetterQueue; }
  public void setPipelineDeadLetterQueue(String value) { pipelineDeadLetterQueue = value; }
}
