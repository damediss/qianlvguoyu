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
 * 本地均不存在时：
 * <ul>
 *   <li>{@code sign-up/**}（App 扫码上传）→ {@code platform.cdn.public-url}（Nginx 反代的 API 机磁盘）</li>
 *   <li>其它路径 → {@code platform.cdn.remote-fallback-url}（历史 PHP CDN）</li>
 * </ul>
 * 切勿把 App 上传的 sign-up 文件 302 到 cdn.app（那边没有这些对象，会 404 裂图）。
 */
@Component
public class CdnFileServeHelper {

  private final String cdnPath;
  private final String cdnUrl;
  private final String fileLocalRoot;
  private final String remoteFallbackUrl;
  private final String publicCdnUrl;

  public CdnFileServeHelper(
      FileStorageProperties fileStorageProperties,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.cdn.remote-fallback-url:https://cdn.app.gjjd168.com}") String remoteFallbackUrl,
      @Value("${platform.cdn.public-url:}") String publicCdnUrl) {
    this.cdnPath = cdnPath;
    this.cdnUrl = trimTrailingSlash(cdnUrl);
    this.fileLocalRoot = fileStorageProperties.localRoot();
    this.remoteFallbackUrl = trimTrailingSlash(remoteFallbackUrl);
    this.publicCdnUrl = trimTrailingSlash(publicCdnUrl);
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
   * 返回浏览器可访问的 URL：本地存在走 cdn.url；否则走合适的远端回退。
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
    return remoteFallbackUrl(relative).orElse(cdnUrl + "/" + relative);
  }

  /**
   * 本地无文件时的 302 目标。App 扫码上传（sign-up/**）必须回退到 public-url，不能用历史 PHP CDN。
   */
  public Optional<String> remoteFallbackUrl(String relativePath) {
    String relative = normalizeRelative(relativePath);
    if (relative == null) {
      return Optional.empty();
    }
    if (resolveLocalPath(relative).isPresent()) {
      return Optional.empty();
    }
    if (isAppUploadRelative(relative)) {
      if (!StringUtils.hasText(publicCdnUrl)) {
        return Optional.empty();
      }
      return Optional.of(publicCdnUrl + "/" + relative);
    }
    if (!StringUtils.hasText(remoteFallbackUrl)) {
      return Optional.empty();
    }
    return Optional.of(remoteFallbackUrl + "/" + relative);
  }

  /** App 扫码采集上传目录（文件在 API 机磁盘，经 Nginx 提供，不在 cdn.app） */
  private static boolean isAppUploadRelative(String relative) {
    return relative.startsWith("sign-up/") || relative.startsWith("sign-up\\");
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

  private static String trimTrailingSlash(String url) {
    if (!StringUtils.hasText(url)) {
      return "";
    }
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
