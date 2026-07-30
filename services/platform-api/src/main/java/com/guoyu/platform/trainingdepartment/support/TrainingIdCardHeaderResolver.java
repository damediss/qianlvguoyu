package com.guoyu.platform.trainingdepartment.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 学员证件照 URL，对齐 PHP YuntrainController::classstudentlistAction。
 *
 * <p>优先级：本地 CDN 文件 &gt; 用户详情 &gt; 报名人员已存 URL。
 * 本地无文件时不再伪造 idCard/headerOrigin/{id}_header2.jpg（会 302 到生产 CDN 再 404，列表整列裂图）。
 */
@Component
public class TrainingIdCardHeaderResolver {

  private final String cdnPath;
  /** 本机直连/开发用（可含 :8080） */
  private final String cdnUrl;
  /** 浏览器公网根地址（Nginx，不含 :8080）；为空则对非本机 URL 去掉内网端口 */
  private final String publicCdnUrl;

  public TrainingIdCardHeaderResolver(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.cdn.public-url:}") String publicCdnUrl) {
    this.cdnPath = cdnPath;
    this.cdnUrl = trimTrailingSlash(cdnUrl);
    this.publicCdnUrl = StringUtils.hasText(publicCdnUrl) ? trimTrailingSlash(publicCdnUrl) : "";
  }

  /** 班级学员列表证件照：本地文件 > 用户详情 > 报名人员；均无则空串 */
  public String resolveForStudent(String idCard, String detailHeader, String signUpHeader) {
    String fromFile = resolveHeaderUrl(idCard);
    if (StringUtils.hasText(fromFile)) {
      return fromFile;
    }
    String fromDetail = resolveStoredHeader(detailHeader);
    if (StringUtils.hasText(fromDetail)) {
      return fromDetail;
    }
    String fromSignUp = resolveStoredHeader(signUpHeader);
    if (StringUtils.hasText(fromSignUp)) {
      return fromSignUp;
    }
    return "";
  }

  public String resolveHeaderUrl(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return "";
    }
    String id = idCard.trim();
    Path root = Paths.get(cdnPath).toAbsolutePath().normalize();

    Path oldFull = root.resolve("idCard/old/headerOrigin").resolve(id + "_full.png");
    if (Files.exists(oldFull)) {
      return browserCdnRoot() + "/idCard/old/headerOrigin/" + id + "_full.png";
    }
    Path header2 = root.resolve("idCard/headerOrigin").resolve(id + "_header2.jpg");
    if (Files.exists(header2)) {
      return browserCdnRoot() + "/idCard/headerOrigin/" + id + "_header2.jpg";
    }
    Path header = root.resolve("idCard/headerOrigin").resolve(id + "_header.jpg");
    if (Files.exists(header)) {
      return browserCdnRoot() + "/idCard/headerOrigin/" + id + "_header.jpg";
    }
    return "";
  }

  /** 将 DB 存储值转为浏览器可访问的 URL */
  public String resolveStoredHeader(String stored) {
    if (!StringUtils.hasText(stored) || isInvalidImageStored(stored)) {
      return "";
    }
    String trimmed = stored.trim();
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      return stripInternalServicePort(trimmed);
    }
    if (trimmed.startsWith("/api/")) {
      return trimmed;
    }
    if (trimmed.startsWith("api/")) {
      return "/" + trimmed;
    }
    String normalized = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    Path rel = Paths.get(cdnPath, normalized);
    if (Files.exists(rel)) {
      return browserCdnRoot() + "/" + normalized;
    }
    // App 扫码上传等已落在远端磁盘：相对路径用公网 CDN 根拼接；无公网根则交给前端同源代理
    if (StringUtils.hasText(publicCdnUrl)) {
      return publicCdnUrl + "/" + normalized;
    }
    return cdnUrl + "/" + normalized;
  }

  /** 列表展示用 CDN 根：优先公网 Nginx 地址，否则本机 cdn.url */
  private String browserCdnRoot() {
    return StringUtils.hasText(publicCdnUrl) ? publicCdnUrl : cdnUrl;
  }

  /**
   * 公网预览时去掉非本机 :8080/:8081（直连常被防火墙拦截，应走 Nginx 80/443）。
   * App 上传常写入 http://host:8080/api/exam/files/...
   */
  private String stripInternalServicePort(String absoluteUrl) {
    try {
      java.net.URI uri = java.net.URI.create(absoluteUrl);
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
      return absoluteUrl;
    } catch (Exception ex) {
      return absoluteUrl;
    }
  }

  private static String trimTrailingSlash(String url) {
    if (!StringUtils.hasText(url)) {
      return "";
    }
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private boolean isInvalidImageStored(String path) {
    String t = path.trim();
    return t.startsWith("process=image")
        || t.startsWith("x-oss-process=")
        || t.startsWith("?x-oss-process=")
        || t.matches("^(image/)?(resize|quality|crop).*");
  }
}
