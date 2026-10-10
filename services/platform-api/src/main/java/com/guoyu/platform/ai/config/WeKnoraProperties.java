package com.guoyu.platform.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "weknora")
public class WeKnoraProperties {

  private String baseUrl;
  private String channelId;
  private String publishToken;
  private String origin;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getPublishToken() {
    return publishToken;
  }

  public void setPublishToken(String publishToken) {
    this.publishToken = publishToken;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }
}
