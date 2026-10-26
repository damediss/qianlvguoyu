package com.guoyu.platform.competition.integration.plantvr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.competition.plantvr")
public class PlantVrCompetitionProperties {
  private boolean enabled;
  private String baseUrl = "http://127.0.0.1:8189/api";
  private String internalSecret;
  private int connectTimeoutSeconds = 5;
  private int readTimeoutSeconds = 30;
  private int pageSize = 100;

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean value) { enabled = value; }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String value) { baseUrl = value; }
  public String getInternalSecret() { return internalSecret; }
  public void setInternalSecret(String value) { internalSecret = value; }
  public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
  public void setConnectTimeoutSeconds(int value) { connectTimeoutSeconds = value; }
  public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
  public void setReadTimeoutSeconds(int value) { readTimeoutSeconds = value; }
  public int getPageSize() { return pageSize; }
  public void setPageSize(int value) { pageSize = value; }
}
