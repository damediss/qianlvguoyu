package com.guoyu.platform.competition.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.competition.messaging")
public class CompetitionMessagingProperties {

  private boolean enabled;
  private long dispatchDelayMillis = 5000L;
  private int batchSize = 50;
  private int maxAttempts = 10;
  private String exchange = "competition.events";
  private String routingKey = "competition.notice.published.v1";
  private String queue = "competition.notice.delivery";
  private String deadLetterQueue = "competition.notice.delivery.dlq";
  private String deadLetterExchange = "competition.dlx";

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public long getDispatchDelayMillis() { return dispatchDelayMillis; }
  public void setDispatchDelayMillis(long dispatchDelayMillis) { this.dispatchDelayMillis = dispatchDelayMillis; }
  public int getBatchSize() { return batchSize; }
  public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
  public int getMaxAttempts() { return maxAttempts; }
  public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
  public String getExchange() { return exchange; }
  public void setExchange(String exchange) { this.exchange = exchange; }
  public String getRoutingKey() { return routingKey; }
  public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
  public String getQueue() { return queue; }
  public void setQueue(String queue) { this.queue = queue; }
  public String getDeadLetterQueue() { return deadLetterQueue; }
  public void setDeadLetterQueue(String deadLetterQueue) { this.deadLetterQueue = deadLetterQueue; }
  public String getDeadLetterExchange() { return deadLetterExchange; }
  public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }
}
