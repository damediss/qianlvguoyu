package com.guoyu.platform.file;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.file.config.FileStorageProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.util.StringUtils;

/**
 * 腾讯云 COS 文件存储。
 *
 * 依赖 com.qcloud.cos:cos_api(在 pom.xml 中加)。
 * 配置示例:
 *   FILE_STORAGE_PROVIDER=cos
 *   COS_SECRET_ID=AKIDxxx
 *   COS_SECRET_KEY=xxx
 *   COS_REGION=ap-guangzhou
 *   COS_BUCKET=guoyu-test-12345
 *   COS_URL_PREFIX=https://guoyu-test-12345.cos.ap-guangzhou.myqcloud.com   (可选,
 *     默认按 region + bucket 拼;要 CDN 走 https://cdn.your.domain 时这里填 CDN 域名)
 */
@Slf4j
public class TencentCosFileStorage implements FileStorage {

  private final FileStorageProperties properties;
  private final COSClient client;
  private final String bucket;
  private final String urlPrefix;

  public TencentCosFileStorage(FileStorageProperties properties) {
    this.properties = properties;
    FileStorageProperties.Cos c = properties.cos();
    if (!StringUtils.hasText(c.secretId()) || !StringUtils.hasText(c.secretKey())
        || !StringUtils.hasText(c.bucket())) {
      throw new IllegalStateException(
          "COS 配置缺失:COS_SECRET_ID / COS_SECRET_KEY / COS_BUCKET 必须全部设置");
    }
    COSCredentials creds = new BasicCOSCredentials(c.secretId(), c.secretKey());
    ClientConfig config = new ClientConfig(new Region(c.region()));
    this.client = new COSClient(creds, config);
    this.bucket = c.bucket();
    this.urlPrefix = StringUtils.hasText(c.urlPrefix())
        ? c.urlPrefix()
        : String.format("https://%s.cos.%s.myqcloud.com", c.bucket(), c.region());
    log.info("[TencentCosFileStorage] 已初始化 bucket={} region={} urlPrefix={}", bucket, c.region(), urlPrefix);
  }

  @Override
  public String put(String objectKey, InputStream content, long sizeBytes, String contentType) {
    // COS SDK 会消费流；先缓冲以便本地镜像双写（下载时可本地回退）
    byte[] bytes;
    try {
      bytes = content.readAllBytes();
    } catch (IOException ex) {
      throw new RuntimeException("COS put read failed: " + objectKey, ex);
    }
    ObjectMetadata meta = new ObjectMetadata();
    meta.setContentLength(bytes.length);
    if (StringUtils.hasText(contentType)) {
      meta.setContentType(contentType);
    }
    PutObjectRequest req = new PutObjectRequest(
        bucket, objectKey, new java.io.ByteArrayInputStream(bytes), meta);
    PutObjectResult result = client.putObject(req);
    if (result == null || result.getETag() == null) {
      throw new RuntimeException("COS put returned no ETag for " + objectKey);
    }
    mirrorLocal(objectKey, bytes);
    return joinUrl(urlPrefix, objectKey);
  }

  private void mirrorLocal(String objectKey, byte[] bytes) {
    try {
      Path root = java.nio.file.Paths.get(properties.localRoot()).toAbsolutePath().normalize();
      Path target = root.resolve(objectKey).normalize();
      if (!target.startsWith(root)) {
        log.warn("[TencentCosFileStorage] skip local mirror invalid key: {}", objectKey);
        return;
      }
      java.nio.file.Files.createDirectories(target.getParent());
      java.nio.file.Files.write(target, bytes);
    } catch (Exception ex) {
      log.warn("[TencentCosFileStorage] local mirror failed: {} - {}", objectKey, ex.toString());
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      client.deleteObject(bucket, objectKey);
    } catch (Exception ex) {
      log.warn("[TencentCosFileStorage] delete failed: {} - {}", objectKey, ex.toString());
    }
  }

  @Override
  public String providerName() {
    return "cos";
  }

  /**
   * 取对象流(供后端 /api/exam/files/{id} 反向流式代理 COS,避免给浏览器暴露 COS 路径
   * 同时也能继续保持 /exam/files/** permitAll 走 Sa-Token 之外).
   */
  public COSObject getObject(String objectKey) {
    return client.getObject(new GetObjectRequest(bucket, objectKey));
  }

  private String joinUrl(String prefix, String key) {
    String p = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    return p + "/" + key;
  }
}
