package com.guoyu.platform.file.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.file")
public record FileStorageProperties(
    String storageProvider,
    String localRoot,
    String objectKeyPrefix,
    Cos cos
) {
  public record Cos(
      String secretId,
      String secretKey,
      String region,
      String bucket,
      String urlPrefix
  ) {}
}
