package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.support.CdnFileServeHelper;
import com.guoyu.platform.trainingdepartment.support.TrainingRemoteImageUtil.ImageBytes;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 默认模板导出素材解析：平台文件 ID / 本机 CDN 目录 / 绝对路径 / http(s) URL。
 *
 * <p>取值顺序与 {@code /api/exam/files/**}（CdnFileServeHelper）保持一致：先查本机磁盘
 * （platform.cdn.path、platform.file.local-root），本地没有才走远端 CDN。
 *
 * <p>本机 loopback（localhost/127.0.0.1）的 {@code /api/exam/files} 不再回环拉取：
 * 与本机磁盘是同一批根目录，本地没有时再 HTTP 自己只会空等或 302 空转。历史上生产把
 * platform.cdn.url 配成「公网 IP:8080」（8080 未对外开放），每个候选白等连接超时。
 *
 * <p>非 loopback 的 public-url（例如本机连生产导出）仍允许 HTTP 拉取：本地磁盘没有、
 * 公网 Nginx 有文件时，否则毕业证/等级证等库字段有值也会静默丢图。
 */
@Component
@Slf4j
public class SignUpExportAssetService {

  private static final Pattern FILE_API_PATTERN =
      Pattern.compile("/api/files/(\\d+)(?:/download)?");

  /** 平台自己的文件端点片段：命中即视为相对路径，先查本机磁盘。 */
  private static final String EXAM_FILES_SEGMENT = "/exam/files/";

  private final CdnFileServeHelper cdnFileServeHelper;
  private final TrainingRemoteImageUtil remoteImageUtil;
  private final FileApplicationService fileApplicationService;
  private final List<String> selfFileUrlPrefixes;
  private final String appBaseUrl;

  public SignUpExportAssetService(
      CdnFileServeHelper cdnFileServeHelper,
      TrainingRemoteImageUtil remoteImageUtil,
      FileApplicationService fileApplicationService,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.cdn.public-url:}") String publicUrl,
      @Value("${platform.app-base-url:}") String appBaseUrl) {
    this.cdnFileServeHelper = cdnFileServeHelper;
    this.remoteImageUtil = remoteImageUtil;
    this.fileApplicationService = fileApplicationService;
    this.selfFileUrlPrefixes = List.of(trimTrailingSlash(cdnUrl), trimTrailingSlash(publicUrl));
    this.appBaseUrl = trimTrailingSlash(appBaseUrl);
  }

  /** 一次导出共用一个解析器：同一候选路径不重复探测、不重复下载（名单里同一人可能出现多行）。 */
  public AssetResolver newResolver() {
    return new AssetResolver();
  }

  public boolean copyTo(String storedOrUrl, Path target) {
    return newResolver().copyTo(storedOrUrl, target);
  }

  public boolean copyFirstExisting(Path target, List<String> candidates) {
    return newResolver().copyFirstExisting(target, candidates);
  }

  public boolean copyFirstExisting(Path target, String... candidates) {
    return newResolver().copyFirstExisting(target, Arrays.asList(candidates));
  }

  public boolean existsLocally(String storedPath) {
    return newResolver().existsLocally(storedPath);
  }

  /** 单次导出运行内的素材解析器，带命中/未命中缓存。 */
  public final class AssetResolver {

    private final Map<String, Path> localHits = new HashMap<>();
    private final Set<String> misses = new HashSet<>();

    /** 按候选源依次尝试，成功则写入 target。 */
    public boolean copyFirstExisting(Path target, List<String> candidates) {
      if (target == null || candidates == null) {
        return false;
      }
      for (String candidate : candidates) {
        if (StringUtils.hasText(candidate) && copyTo(candidate, target)) {
          return true;
        }
      }
      return false;
    }

    public boolean copyTo(String storedOrUrl, Path target) {
      if (!StringUtils.hasText(storedOrUrl) || target == null) {
        return false;
      }
      String src = storedOrUrl.trim();
      try {
        Files.createDirectories(target.getParent());

        Matcher api = FILE_API_PATTERN.matcher(src);
        if (api.find()) {
          return copyFileId(Long.parseLong(api.group(1)), target);
        }

        String selfRelative = toSelfRelative(src);
        if (selfRelative != null) {
          return copyRelative(selfRelative, target);
        }

        if (isHttp(src)) {
          return copyRemote(src, target);
        }

        // 绝对本地路径
        try {
          Path absolute = Path.of(src);
          if (absolute.isAbsolute()) {
            if (Files.isRegularFile(absolute)) {
              Files.copy(absolute, target, StandardCopyOption.REPLACE_EXISTING);
              return true;
            }
            return false;
          }
        } catch (InvalidPathException ignored) {
          // Windows 下带 ? 的存储值解析不了，按相对路径继续（normalizeRelative 会去掉 query）
        }

        return copyRelative(src, target);
      } catch (Exception e) {
        log.warn("导出素材拷贝失败 src={} target={}", src, target, e);
        return false;
      }
    }

    /** 候选路径是否在本机磁盘上（不触发网络请求）。 */
    public boolean existsLocally(String storedPath) {
      if (!StringUtils.hasText(storedPath)) {
        return false;
      }
      String src = storedPath.trim();
      String relative = toSelfRelative(src);
      if (relative == null) {
        if (isHttp(src)) {
          return false;
        }
        try {
          Path absolute = Path.of(src);
          if (absolute.isAbsolute()) {
            return Files.isRegularFile(absolute);
          }
        } catch (InvalidPathException ignored) {
          return false;
        }
        relative = normalizeRelative(src);
      }
      return relative != null && cdnFileServeHelper.resolveLocalPath(relative).isPresent();
    }

    private boolean copyFileId(Long fileId, Path target) throws Exception {
      String key = "file-id:" + fileId;
      if (misses.contains(key)) {
        return false;
      }
      byte[] bytes = fileApplicationService.readFileBytesById(fileId);
      if (bytes != null && bytes.length > 0) {
        Files.write(target, bytes);
        return true;
      }
      // 本机磁盘无对象时（常见：连生产库但在开发机导出），经公网 API 再拉一次
      String remote = fileDownloadFallbackUrl(fileId);
      if (remote != null && copyRemote(remote, target)) {
        return true;
      }
      misses.add(key);
      return false;
    }

    /** 相对路径：本机磁盘（cdn.path / file.local-root）优先，本地没有才回退远端 CDN。 */
    private boolean copyRelative(String relative, Path target) throws Exception {
      String key = normalizeRelative(relative);
      if (key == null || misses.contains(key)) {
        return false;
      }
      Path cached = localHits.get(key);
      if (cached != null && Files.isRegularFile(cached)) {
        Files.copy(cached, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
      }
      Optional<Path> local = cdnFileServeHelper.resolveLocalPath(key);
      if (local.isPresent()) {
        localHits.put(key, local.get());
        Files.copy(local.get(), target, StandardCopyOption.REPLACE_EXISTING);
        return true;
      }
      Optional<String> remote = cdnFileServeHelper.remoteFallbackUrl(key);
      // loopback 自称地址跳过；公网 public-url / 历史 CDN 仍拉取
      if (remote.isPresent() && !isLoopbackHttpUrl(remote.get()) && copyRemote(remote.get(), target)) {
        return true;
      }
      misses.add(key);
      return false;
    }

    private boolean copyRemote(String url, Path target) throws Exception {
      if (misses.contains(url)) {
        return false;
      }
      Optional<ImageBytes> fetched = remoteImageUtil.fetchImage(url);
      if (fetched.isEmpty()) {
        misses.add(url);
        return false;
      }
      Files.write(target, fetched.get().data());
      return true;
    }
  }

  /**
   * 识别指向本服务 {@code /api/exam/files/**} 的链接并还原成相对路径。
   * 配置漂移（域名/IP 换过）时用 {@code /exam/files/} 片段兜底。
   */
  private String toSelfRelative(String src) {
    for (String prefix : selfFileUrlPrefixes) {
      if (StringUtils.hasText(prefix) && src.startsWith(prefix + "/")) {
        return normalizeRelative(src.substring(prefix.length() + 1));
      }
    }
    if (isHttp(src)) {
      int idx = src.indexOf(EXAM_FILES_SEGMENT);
      if (idx >= 0) {
        return normalizeRelative(src.substring(idx + EXAM_FILES_SEGMENT.length()));
      }
      return null;
    }
    return null;
  }

  /**
   * 平台文件 ID 的公网下载地址：优先 app-base-url；否则从非 loopback 的 cdn/public-url 推导源站。
   */
  String fileDownloadFallbackUrl(long fileId) {
    if (StringUtils.hasText(appBaseUrl) && !isLoopbackHttpUrl(appBaseUrl)) {
      return appBaseUrl + "/api/files/" + fileId + "/download";
    }
    for (String prefix : selfFileUrlPrefixes) {
      if (!StringUtils.hasText(prefix) || isLoopbackHttpUrl(prefix)) {
        continue;
      }
      int apiIdx = prefix.indexOf("/api/");
      if (apiIdx > 0) {
        return prefix.substring(0, apiIdx) + "/api/files/" + fileId + "/download";
      }
      if (isHttp(prefix)) {
        return prefix + "/api/files/" + fileId + "/download";
      }
    }
    return null;
  }

  /** localhost / 127.0.0.1 回环拉取无意义且易超时；公网「自称 self」地址仍允许 HTTP。 */
  static boolean isLoopbackHttpUrl(String url) {
    if (!StringUtils.hasText(url) || !isHttp(url.trim())) {
      return false;
    }
    try {
      String host = URI.create(url.trim()).getHost();
      return host == null
          || "localhost".equalsIgnoreCase(host)
          || "127.0.0.1".equals(host)
          || "::1".equals(host);
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  private static boolean isHttp(String src) {
    return src.startsWith("http://") || src.startsWith("https://");
  }

  /** 去掉 query、前导斜杠，拒绝目录穿越；空或非法返回 null。 */
  private static String normalizeRelative(String path) {
    if (!StringUtils.hasText(path)) {
      return null;
    }
    String relative = path.trim();
    if (isHttp(relative)) {
      return null;
    }
    if (relative.startsWith("/")) {
      relative = relative.substring(1);
    }
    int q = relative.indexOf('?');
    if (q >= 0) {
      relative = relative.substring(0, q);
    }
    if (relative.isEmpty() || relative.contains("..")) {
      return null;
    }
    return relative;
  }

  private static String trimTrailingSlash(String url) {
    if (!StringUtils.hasText(url)) {
      return "";
    }
    String trimmed = url.trim();
    return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
  }
}
