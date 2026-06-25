package com.guoyu.platform.file.support;

import com.guoyu.platform.file.config.FileStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 统一解析 /api/exam/files/** 对应的本地磁盘路径与公网 URL。
 *
 * <p>本地读盘顺序：platform.cdn.path（PHP 遗留 CDN）→ platform.file.local-root（新文件模块）。
 * 本地均不存在时，可回退到 platform.cdn.remote-fallback-url（生产 CDN，供本地开发）。
 */
@Component
public class CdnFileServeHelper {

  private final String cdnPath;
  private final String cdnUrl;
  private final String fileLocalRoot;
  private final String remoteFallbackUrl;

  public CdnFileServeHelper(
      FileStorageProperties fileStorageProperties,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.cdn.remote-fallback-url:https://cdn.app.gjjd168.com}") String remoteFallbackUrl) {
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    this.fileLocalRoot = fileStorageProperties.localRoot();
    this.remoteFallbackUrl = StringUtils.hasText(remoteFallbackUrl)
        ? (remoteFallbackUrl.endsWith("/") ? remoteFallbackUrl.substring(0, remoteFallbackUrl.length() - 1) : remoteFallbackUrl)
        : "";
  }

  /** 在 CDN 目录或 file.local-root 下查找可读文件。 */
  public Optional<Path> resolveLocalPath(String relativePath) {
    String relative = normalizeRelative(relativePath);
    if (relative == null) {
      return Optional.empty();
    }
    Optional<Path> fromCdn = resolveUnderRoot(Paths.get(cdnPath), relative);
    if (fromCdn.isPresent()) {
      return fromCdn;
    }
    return resolveUnderRoot(Paths.get(fileLocalRoot), relative);
  }

  /**
   * 返回浏览器可访问的 URL：本地存在走 cdn.url；否则走 remote-fallback（生产 CDN）。
   */
  public String publicUrl(String storedPath) {
    if (!StringUtils.hasText(storedPath)) {
      return null;
    }
    if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) {
      return storedPath;
    }
    String relative = normalizeRelative(storedPath);
    if (relative == null) {
      return null;
    }
    if (resolveLocalPath(relative).isPresent()) {
      return cdnUrl + "/" + relative;
    }
    return remoteFallbackUrl.isEmpty() ? cdnUrl + "/" + relative : remoteFallbackUrl + "/" + relative;
  }

  public Optional<String> remoteFallbackUrl(String relativePath) {
    String relative = normalizeRelative(relativePath);
    if (relative == null || remoteFallbackUrl.isEmpty()) {
      return Optional.empty();
    }
    if (resolveLocalPath(relative).isPresent()) {
      return Optional.empty();
    }
    return Optional.of(remoteFallbackUrl + "/" + relative);
  }

  private static String normalizeRelative(String path) {
    if (!StringUtils.hasText(path)) {
      return null;
    }
    String relative = path.trim();
    if (relative.startsWith("http://") || relative.startsWith("https://")) {
      return null;
    }
    if (relative.startsWith("/")) {
      relative = relative.substring(1);
    }
    if (relative.contains("..")) {
      return null;
    }
    return relative;
  }

  private static Optional<Path> resolveUnderRoot(Path root, String relative) {
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path resolved = normalizedRoot.resolve(relative).normalize();
    if (!resolved.startsWith(normalizedRoot)) {
      return Optional.empty();
    }
    if (!Files.isRegularFile(resolved)) {
      return Optional.empty();
    }
    return Optional.of(resolved);
  }
}
