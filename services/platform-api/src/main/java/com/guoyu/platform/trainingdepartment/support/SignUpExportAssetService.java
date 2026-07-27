package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.trainingdepartment.support.TrainingRemoteImageUtil.ImageBytes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 默认模板导出素材解析：CDN 遗留路径 / 平台文件 ID / http(s) / 相对路径。
 * 对齐 PHP image_url_to_file + headerOrigin 拷贝逻辑。
 */
@Component
@Slf4j
public class SignUpExportAssetService {

  private static final Pattern FILE_API_PATTERN =
      Pattern.compile("/api/files/(\\d+)(?:/download)?");

  private final String cdnPath;
  private final String cdnUrl;
  private final String remoteFallbackUrl;
  private final TrainingRemoteImageUtil remoteImageUtil;
  private final FileApplicationService fileApplicationService;

  public SignUpExportAssetService(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.cdn.remote-fallback-url:https://cdn.app.gjjd168.com}") String remoteFallbackUrl,
      TrainingRemoteImageUtil remoteImageUtil,
      FileApplicationService fileApplicationService) {
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    this.remoteFallbackUrl = remoteFallbackUrl != null && remoteFallbackUrl.endsWith("/")
        ? remoteFallbackUrl.substring(0, remoteFallbackUrl.length() - 1)
        : remoteFallbackUrl;
    this.remoteImageUtil = remoteImageUtil;
    this.fileApplicationService = fileApplicationService;
  }

  /** 按候选源依次尝试，成功则写入 target（扩展名固定为 .jpg 时由调用方决定）。 */
  public boolean copyFirstExisting(Path target, String... candidates) {
    if (target == null || candidates == null) {
      return false;
    }
    for (String candidate : candidates) {
      if (copyTo(candidate, target)) {
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
        Long fileId = Long.parseLong(api.group(1));
        byte[] bytes = fileApplicationService.readFileBytesById(fileId);
        if (bytes != null && bytes.length > 0) {
          Files.write(target, bytes);
          return true;
        }
        return false;
      }

      // CDN 相对路径
      Path local = resolveCdnRelative(src);
      if (local != null && Files.isRegularFile(local)) {
        Files.copy(local, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
      }

      // 绝对本地路径
      Path absolute = Path.of(src);
      if (absolute.isAbsolute() && Files.isRegularFile(absolute)) {
        Files.copy(absolute, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
      }

      // http(s) 或本地 CDN URL / 生产 CDN 回退
      if (src.startsWith("http://") || src.startsWith("https://")) {
        Optional<ImageBytes> fetched = remoteImageUtil.fetchImage(src);
        if (fetched.isPresent()) {
          Files.write(target, fetched.get().data());
          return true;
        }
        return false;
      }

      String relative = src.startsWith("/") ? src.substring(1) : src;
      int q = relative.indexOf('?');
      if (q >= 0) {
        relative = relative.substring(0, q);
      }
      Optional<ImageBytes> viaLocalCdn = remoteImageUtil.fetchImage(cdnUrl + "/" + relative);
      if (viaLocalCdn.isPresent()) {
        Files.write(target, viaLocalCdn.get().data());
        return true;
      }
      if (StringUtils.hasText(remoteFallbackUrl)) {
        Optional<ImageBytes> viaRemote = remoteImageUtil.fetchImage(remoteFallbackUrl + "/" + relative);
        if (viaRemote.isPresent()) {
          Files.write(target, viaRemote.get().data());
          return true;
        }
      }
    } catch (Exception e) {
      log.warn("导出素材拷贝失败 src={} target={}", src, target, e);
    }
    return false;
  }

  private Path resolveCdnRelative(String src) {
    if (src.startsWith("http://") || src.startsWith("https://")) {
      // 尝试把 CDN URL 还原成相对路径
      if (src.startsWith(cdnUrl + "/")) {
        src = src.substring(cdnUrl.length() + 1);
      } else {
        return null;
      }
    }
    String relative = src.startsWith("/") ? src.substring(1) : src;
    // 去掉 query
    int q = relative.indexOf('?');
    if (q >= 0) {
      relative = relative.substring(0, q);
    }
    Path root = Path.of(cdnPath).toAbsolutePath().normalize();
    Path resolved = root.resolve(relative).normalize();
    if (!resolved.startsWith(root)) {
      return null;
    }
    return resolved;
  }
}
