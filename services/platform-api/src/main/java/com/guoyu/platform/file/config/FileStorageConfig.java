package com.guoyu.platform.file.config;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.file.FileStorage;
import com.guoyu.platform.file.LocalFileStorage;
import com.guoyu.platform.file.TencentCosFileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * FileStorage SPI —— 根据 platform.file.storage-provider 选择 local / cos。
 *
 * 注意:TencentCosFileStorage 类引用了 com.qcloud.cos:cos_api,如果未引入依赖,
 * Bean 创建会失败。生产用 COS 时务必在 pom.xml 加上依赖。
 */
@Configuration
@Slf4j
public class FileStorageConfig {

  @Bean
  public FileStorage fileStorage(
      FileStorageProperties properties,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl
  ) {
    String provider = StringUtils.hasText(properties.storageProvider())
        ? properties.storageProvider().toLowerCase()
        : "local";
    return switch (provider) {
      case "cos" -> {
        log.info("[FileStorage] 使用腾讯云 COS 存储");
        yield new TencentCosFileStorage(properties);
      }
      case "local" -> {
        log.info("[FileStorage] 使用本地磁盘存储,CDN URL = {}", cdnUrl);
        yield new LocalFileStorage(properties, cdnUrl);
      }
      default -> {
        log.warn("[FileStorage] 未知 storage-provider={},回退到 local", provider);
        yield new LocalFileStorage(properties, cdnUrl);
      }
    };
  }
}
