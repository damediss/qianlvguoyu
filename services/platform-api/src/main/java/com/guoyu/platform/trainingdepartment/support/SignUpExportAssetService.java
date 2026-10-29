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

  /** 旧 PHP 合同文件网关：/api/file/contract/{leaf} → 本机 contract/{leaf} */
  private static final String LEGACY_CONTRACT_API_SEGMENT = "/api/file/contract/";

  /**
   * 合同文件名中的历史计划号：{身份证}_{signUpId}_first_self.png
   */
  private static final Pattern CONTRACT_PLAN_ID_PATTERN =
      Pattern.compile("_(\\d+)_(first_self|expire_self|sign_self)\\.(png|jpg|jpeg)$",
          Pattern.CASE_INSENSITIVE);

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
      // 与编辑页展示一致：去掉公网直连常被墙的 :8080/:8081
      String src = stripInternalServicePort(storedOrUrl.trim());
      try {
        Files.createDirectories(target.getParent());

        // /api/files/{id}：只走文件服务，禁止再当相对路径落到旧 headerOrigin 同名盘
        Matcher api = FILE_API_PATTERN.matcher(src);
        if (api.find()) {
          return copyFileId(Long.parseLong(api.group(1)), target);
        }

        // 旧合同网关：/api/file/contract/{leaf} → contract/{leaf}
        String legacyContract = mapLegacyContractApiToRelative(src);
        if (legacyContract != null && copyRelative(legacyContract, target)) {
          return true;
        }

        String selfRelative = toSelfRelative(src);
        if (selfRelative != null) {
          return copyRelative(selfRelative, target);
        }

        if (isHttp(src)) {
          // 线上常见：库里是 https://cdn.app/.../upload/xx.jpg，编辑页浏览器直开 CDN；
          // 导出若只 HTTP 拉外网，API 机出网被墙/超时就会丢图。优先把 URL 还原成相对路径读本机 cdn.path。
          String fromHttp = relativePathFromHttpUrl(src);
          if (fromHttp != null && copyRelative(fromHttp, target)) {
            return true;
          }
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
      // readFileBytesById 缺文件时抛 BusinessException；必须吃掉后再走回退，否则毕业证等会静默丢图
      try {
        byte[] bytes = fileApplicationService.readFileBytesById(fileId);
        if (bytes != null && bytes.length > 0) {
          Files.write(target, bytes);
          return true;
        }
      } catch (Exception ex) {
        log.warn("按文件ID读盘失败，尝试公网/exam回退 fileId={} msg={}", fileId, ex.getMessage());
      }

      // 元数据：COS 预签名（免鉴权）/ local 的 exam/files 相对路径读盘或公网拉
      try {
        Optional<String> exportUrl = fileApplicationService.resolveExportFetchUrl(fileId);
        if (exportUrl.isPresent()) {
          String publicFileUrl = stripInternalServicePort(exportUrl.get().trim());
          String examRelative = relativePathFromHttpUrl(publicFileUrl);
          if (examRelative == null) {
            examRelative = toSelfRelative(publicFileUrl);
          }
          if (examRelative == null && publicFileUrl.startsWith("/api/exam/files/")) {
            examRelative = normalizeRelative(publicFileUrl.substring("/api/exam/files/".length()));
          }
          if (examRelative != null && copyRelative(examRelative, target)) {
            return true;
          }
          if (isHttp(publicFileUrl) && !isLoopbackHttpUrl(publicFileUrl) && copyRemote(publicFileUrl, target)) {
            return true;
          }
          if (examRelative != null) {
            for (String prefix : selfFileUrlPrefixes) {
              if (!StringUtils.hasText(prefix) || isLoopbackHttpUrl(prefix)) {
                continue;
              }
              if (copyRemote(prefix + "/" + examRelative, target)) {
                return true;
              }
            }
          }
        }
      } catch (Exception ex) {
        log.warn("按文件ID取导出回退地址失败 fileId={} msg={}", fileId, ex.getMessage());
      }

      // 鉴权 /api/files/{id}/download 公网拉取（可能 401，仅作最后一搏）
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

  /**
   * 从 http(s) CDN/公网地址还原可落盘的相对路径（去掉 /api/exam/files 前缀与 query）。
   * 例：https://cdn.app.gjjd168.com/upload/a.jpg → upload/a.jpg
   * 例：https://api.gjjd168.com/api/file/contract/x_80_first_self.png → contract/x_80_first_self.png
   */
  static String relativePathFromHttpUrl(String url) {
    if (!StringUtils.hasText(url) || !isHttp(url.trim())) {
      return null;
    }
    try {
      String path = URI.create(url.trim()).getPath();
      if (!StringUtils.hasText(path) || "/".equals(path)) {
        return null;
      }
      String legacyContract = mapLegacyContractApiToRelative(path);
      if (legacyContract != null) {
        return legacyContract;
      }
      int examIdx = path.indexOf(EXAM_FILES_SEGMENT);
      if (examIdx >= 0) {
        return normalizeRelative(path.substring(examIdx + EXAM_FILES_SEGMENT.length()));
      }
      // 不要把 /api/files/{id}/download 当成相对磁盘路径
      if (FILE_API_PATTERN.matcher(path).find()) {
        return null;
      }
      return normalizeRelative(path.startsWith("/") ? path.substring(1) : path);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * 库字段 → 可尝试的本机相对路径（合同旧网关 / CDN upload / exam files）。
   * 供打包侧在「库字段有记录」时把映射路径列为显式候选。
   */
  static String mapStoredToLocalRelative(String storedOrUrl) {
    if (!StringUtils.hasText(storedOrUrl)) {
      return null;
    }
    String src = storedOrUrl.trim();
    String legacyContract = mapLegacyContractApiToRelative(src);
    if (legacyContract != null) {
      return legacyContract;
    }
    if (isHttp(src)) {
      return relativePathFromHttpUrl(src);
    }
    if (FILE_API_PATTERN.matcher(src).find()) {
      return null;
    }
    return normalizeRelative(src);
  }

  /**
   * 从合同存储值解析历史 signUpId（文件名 …_{planId}_first_self.png）。
   */
  static Long parseContractSignUpId(String storedOrUrl) {
    if (!StringUtils.hasText(storedOrUrl)) {
      return null;
    }
    String leaf = contractLeafName(storedOrUrl.trim());
    if (leaf == null) {
      return null;
    }
    Matcher m = CONTRACT_PLAN_ID_PATTERN.matcher(leaf);
    if (!m.find()) {
      return null;
    }
    try {
      return Long.parseLong(m.group(1));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  /** 取合同 URL/路径的文件名部分。 */
  static String contractLeafName(String storedOrUrl) {
    if (!StringUtils.hasText(storedOrUrl)) {
      return null;
    }
    String src = storedOrUrl.trim();
    try {
      if (isHttp(src)) {
        String path = URI.create(src).getPath();
        if (!StringUtils.hasText(path)) {
          return null;
        }
        src = path;
      }
    } catch (IllegalArgumentException ex) {
      // keep src
    }
    int slash = Math.max(src.lastIndexOf('/'), src.lastIndexOf('\\'));
    String leaf = slash >= 0 ? src.substring(slash + 1) : src;
    int q = leaf.indexOf('?');
    if (q >= 0) {
      leaf = leaf.substring(0, q);
    }
    return StringUtils.hasText(leaf) ? leaf : null;
  }

  /**
   * {@code /api/file/contract/{leaf}} → {@code contract/{leaf}}；
   * 勿还原成 {@code api/file/contract/...}（会在 cdn 根下找不到文件）。
   */
  static String mapLegacyContractApiToRelative(String storedOrUrl) {
    if (!StringUtils.hasText(storedOrUrl)) {
      return null;
    }
    String src = storedOrUrl.trim();
    String path = src;
    if (isHttp(src)) {
      try {
        path = URI.create(src).getPath();
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
    if (!StringUtils.hasText(path)) {
      return null;
    }
    int idx = path.indexOf(LEGACY_CONTRACT_API_SEGMENT);
    if (idx < 0) {
      return null;
    }
    String leaf = path.substring(idx + LEGACY_CONTRACT_API_SEGMENT.length());
    return normalizeRelative("contract/" + leaf);
  }

  /**
   * 公网预览时去掉非本机 :8080/:8081（与 SignUpPeopleSelfAssembler 展示口径一致）。
   * 导出若仍打带端口地址，常被防火墙拦截导致毕业证等拷不到。
   */
  static String stripInternalServicePort(String absoluteUrl) {
    if (!StringUtils.hasText(absoluteUrl) || !isHttp(absoluteUrl.trim())) {
      return absoluteUrl;
    }
    try {
      URI uri = URI.create(absoluteUrl.trim());
      String host = uri.getHost();
      int port = uri.getPort();
      boolean local = host == null
          || "localhost".equalsIgnoreCase(host)
          || "127.0.0.1".equals(host);
      if (!local && (port == 8080 || port == 8081)) {
        String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        if (uri.getRawPath() != null) {
          sb.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
          sb.append('?').append(uri.getRawQuery());
        }
        return sb.toString();
      }
    } catch (IllegalArgumentException ignored) {
      // keep original
    }
    return absoluteUrl;
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
