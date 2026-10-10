package com.guoyu.platform.trainingdepartment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OnlyOffice Document Server 对接配置。
 *
 * <p>Document Server 必须能访问 {@code publicBaseUrl}（下载文件与回调），
 * 浏览器访问 {@code documentServerUrl} 加载编辑器脚本。
 */
@ConfigurationProperties(prefix = "platform.onlyoffice")
public record OnlyOfficeProperties(
    boolean enabled,
    /** 浏览器访问 Document Server 的根地址，如 http://localhost:8088 */
    String documentServerUrl,
    /**
     * Document Server 访问本 API 的公网/可达基址（含 /api），
     * Docker 本机常用 http://host.docker.internal:8080/api
     */
    String publicBaseUrl,
    boolean jwtEnabled,
    String jwtSecret,
    /** 文档下载签名有效期（秒） */
    int downloadTokenTtlSeconds
) {
  public OnlyOfficeProperties {
    if (downloadTokenTtlSeconds <= 0) {
      downloadTokenTtlSeconds = 3600;
    }
  }

  public String normalizedDocumentServerUrl() {
    if (documentServerUrl == null || documentServerUrl.isBlank()) {
      return "";
    }
    return documentServerUrl.replaceAll("/+$", "");
  }

  public String normalizedPublicBaseUrl() {
    if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
      return "";
    }
    return publicBaseUrl.replaceAll("/+$", "");
  }

  public String apiScriptUrl() {
    String base = normalizedDocumentServerUrl();
    if (base.isEmpty()) {
      return "";
    }
    return base + "/web-apps/apps/api/documents/api.js";
  }
}
