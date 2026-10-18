package com.guoyu.platform.exam.zhenti.service;

import com.guoyu.platform.exam.zhenti.vo.ZhentiPreviewVO;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 预览地址拼装（方案 §2.7）。
 *
 * <p>viewer 与 F1 同域（R6）；file 参数为带签名的完整 F1 URL，由后端拼好，App/前端不感知细节。
 */
@Component
public class ZhentiViewerUrlBuilder {

  private final ZhentiSignatureService signatureService;
  private final String viewerOrigin;
  private final String apiBase;

  public ZhentiViewerUrlBuilder(
      ZhentiSignatureService signatureService,
      @Value("${platform.zhenti.viewer-origin:https://java.app.gjjd168.com}") String viewerOrigin,
      @Value("${platform.zhenti.api-base:/api}") String apiBase) {
    this.signatureService = signatureService;
    this.viewerOrigin = trimTrailingSlash(viewerOrigin);
    this.apiBase = normalizeBase(apiBase);
  }

  public ZhentiPreviewVO build(Long id, String title) {
    ZhentiSignatureService.SignedUrl signed = signatureService.sign(id);
    String fileUrl = viewerOrigin + apiBase + "/appzhenti/file/" + id
        + "?sign=" + signed.sign() + "&t=" + signed.expiresAt();
    String encoded = URLEncoder.encode(fileUrl, StandardCharsets.UTF_8).replace("+", "%20");
    return new ZhentiPreviewVO(viewerOrigin + "/pdf-viewer/viewer.html?file=" + encoded, title);
  }

  private static String trimTrailingSlash(String value) {
    String trimmed = value == null ? "" : value.trim();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  private static String normalizeBase(String value) {
    String base = value == null || value.isBlank() ? "/api" : value.trim();
    if (!base.startsWith("/")) {
      base = "/" + base;
    }
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base;
  }
}
