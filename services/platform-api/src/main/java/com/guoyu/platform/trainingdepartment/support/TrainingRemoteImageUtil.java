package com.guoyu.platform.trainingdepartment.support;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 远程/本地图片拉取，对齐 PHP image_url_to_file */
@Component
@Slf4j
public class TrainingRemoteImageUtil {

  private final String cdnPath;
  private final String cdnUrl;
  private final HttpClient httpClient;
  private final Duration readTimeout;

  public TrainingRemoteImageUtil(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.cdn.fetch-connect-timeout-seconds:3}") long connectTimeoutSeconds,
      @Value("${platform.cdn.fetch-read-timeout-seconds:30}") long readTimeoutSeconds) {
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    this.readTimeout = Duration.ofSeconds(readTimeoutSeconds);
    // 连接超时按秒级收敛：拉图属于可失败的旁路素材，不该让导出线程长时间空等
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  public record ImageBytes(byte[] data, String extension) {}

  public Optional<ImageBytes> fetchImage(String src) {
    if (!StringUtils.hasText(src)) {
      return Optional.empty();
    }
    String trimmed = src.trim();
    try {
      if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return fetchHttp(trimmed);
      }
      Path local = resolveLocalPath(trimmed);
      if (local != null && Files.exists(local)) {
        byte[] data = Files.readAllBytes(local);
        String ext = extensionFromPath(local.getFileName().toString());
        if (ext.isEmpty()) {
          ext = detectExtension(data);
        }
        return Optional.of(new ImageBytes(data, ext));
      }
    } catch (Exception e) {
      log.warn("拉取图片失败: {}", trimmed, e);
    }
    return Optional.empty();
  }

  private Optional<ImageBytes> fetchHttp(String url) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(readTimeout)
        .GET()
        .build();
    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      return Optional.empty();
    }
    try (InputStream in = response.body()) {
      byte[] data = in.readAllBytes();
      if (data.length == 0) {
        return Optional.empty();
      }
      String ext = extensionFromPath(url);
      if (ext.isEmpty()) {
        ext = detectExtension(data);
      }
      if (ext.isEmpty()) {
        ext = ".jpg";
      }
      return Optional.of(new ImageBytes(data, ext));
    }
  }

  private Path resolveLocalPath(String path) {
    String relative = path;
    if (relative.startsWith("/")) {
      relative = relative.substring(1);
    }
    Path cdnRoot = Path.of(cdnPath).toAbsolutePath().normalize();
    Path resolved = cdnRoot.resolve(relative).normalize();
    if (resolved.startsWith(cdnRoot) && Files.exists(resolved)) {
      return resolved;
    }
    return null;
  }

  private String extensionFromPath(String path) {
    int q = path.indexOf('?');
    if (q > 0) {
      path = path.substring(0, q);
    }
    int dot = path.lastIndexOf('.');
    if (dot < 0 || dot >= path.length() - 1) {
      return "";
    }
    String ext = path.substring(dot).toLowerCase();
    if (ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
      return ext;
    }
    return "";
  }

  private String detectExtension(byte[] data) {
    if (data.length < 2) {
      return "";
    }
    int b0 = data[0] & 0xFF;
    int b1 = data[1] & 0xFF;
    int code = b0 * 256 + b1;
    return switch (code) {
      case 0xFFD8 -> ".jpg";
      case 0x8950 -> ".png";
      case 0x4749 -> ".gif";
      default -> ".jpg";
    };
  }
}
