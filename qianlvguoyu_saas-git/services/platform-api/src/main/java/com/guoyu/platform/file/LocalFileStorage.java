package com.guoyu.platform.file;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.file.config.FileStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.util.StringUtils;

/**
 * 本地磁盘存储 —— 保留 dev / 单机部署能力。生产 COS 模式见 TencentCosFileStorage。
 *
 * 公网可访问 URL 通过 platform.cdn.url 模板拼出(默认 /api/exam/files/{id} 由后端
 * FileController 暴露);如果用 nginx alias 直接暴露磁盘目录,也可配 CDN_URL 直连。
 */
@Slf4j
public class LocalFileStorage implements FileStorage {

  private final FileStorageProperties properties;
  private final String publicUrlPrefix;

  public LocalFileStorage(FileStorageProperties properties, String publicUrlPrefix) {
    this.properties = properties;
    this.publicUrlPrefix = publicUrlPrefix;
  }

  @Override
  public String put(String objectKey, InputStream content, long sizeBytes, String contentType) {
    Path root = Paths.get(properties.localRoot()).toAbsolutePath().normalize();
    Path target = root.resolve(objectKey).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalArgumentException("Invalid object key: " + objectKey);
    }
    try {
      Files.createDirectories(target.getParent());
      Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new RuntimeException("Local file put failed: " + objectKey, ex);
    }
    return joinUrl(publicUrlPrefix, objectKey);
  }

  @Override
  public void delete(String objectKey) {
    Path root = Paths.get(properties.localRoot()).toAbsolutePath().normalize();
    Path target = root.resolve(objectKey).normalize();
    if (!target.startsWith(root)) {
      log.warn("[LocalFileStorage] skip delete invalid key: {}", objectKey);
      return;
    }
    try {
      Files.deleteIfExists(target);
    } catch (IOException ex) {
      log.warn("[LocalFileStorage] delete failed: {} - {}", objectKey, ex.toString());
    }
  }

  @Override
  public String providerName() {
    return "local";
  }

  private String joinUrl(String prefix, String key) {
    if (!StringUtils.hasText(prefix)) {
      return "/api/exam/files/" + key;  // 后端 FileController 走 DB 查 file_id,需要 id
    }
    String p = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    return p + "/" + key;
  }
}
