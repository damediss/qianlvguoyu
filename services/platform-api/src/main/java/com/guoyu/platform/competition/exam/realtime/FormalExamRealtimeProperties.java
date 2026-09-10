package com.guoyu.platform.competition.exam.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.competition.formal-exam.realtime")
public class FormalExamRealtimeProperties {

  private boolean enabled = true;
  private int ticketTtlSeconds = 45;
  private int heartbeatSeconds = 20;
  private int presenceTtlSeconds = 60;
  private int sendQueueLimit = 32;
  private int dispatchDelayMillis = 1000;
  private int dispatchBatchSize = 50;
  private int dispatchMaxAttempts = 8;
  private String nodeId = "";
  private String wsPath = "/api/ws/competition/formal-exam/player";
  private String consoleWsPath = "/api/ws/competition/formal-exam/console";

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public int getTicketTtlSeconds() { return ticketTtlSeconds; }
  public void setTicketTtlSeconds(int ticketTtlSeconds) { this.ticketTtlSeconds = ticketTtlSeconds; }
  public int getHeartbeatSeconds() { return heartbeatSeconds; }
  public void setHeartbeatSeconds(int heartbeatSeconds) { this.heartbeatSeconds = heartbeatSeconds; }
  public int getPresenceTtlSeconds() { return presenceTtlSeconds; }
  public void setPresenceTtlSeconds(int presenceTtlSeconds) { this.presenceTtlSeconds = presenceTtlSeconds; }
  public int getSendQueueLimit() { return sendQueueLimit; }
  public void setSendQueueLimit(int sendQueueLimit) { this.sendQueueLimit = sendQueueLimit; }
  public int getDispatchDelayMillis() { return dispatchDelayMillis; }
  public void setDispatchDelayMillis(int dispatchDelayMillis) { this.dispatchDelayMillis = dispatchDelayMillis; }
  public int getDispatchBatchSize() { return dispatchBatchSize; }
  public void setDispatchBatchSize(int dispatchBatchSize) { this.dispatchBatchSize = dispatchBatchSize; }
  public int getDispatchMaxAttempts() { return dispatchMaxAttempts; }
  public void setDispatchMaxAttempts(int dispatchMaxAttempts) { this.dispatchMaxAttempts = dispatchMaxAttempts; }
  public String getNodeId() { return nodeId; }
  public void setNodeId(String nodeId) { this.nodeId = nodeId; }
  public String getWsPath() { return wsPath; }
  public void setWsPath(String wsPath) { this.wsPath = wsPath; }
  public String getConsoleWsPath() { return consoleWsPath; }
  public void setConsoleWsPath(String consoleWsPath) { this.consoleWsPath = consoleWsPath; }
}
