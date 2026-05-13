package com.guoyu.platform.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.file")
public record FileStorageProperties(
    String storageProvider,
    String localRoot
) {
}
