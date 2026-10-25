package com.guoyu.platform.file.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.entity.FileEntity;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;

/**
 * 受保护文件区（protected/**）统一访问闸门（教材二期方案 §5.6）。
 *
 * <p>受保护对象 key 以 {@code protected/} 开头（如 {@code protected/textbook/{tenantId}/...}）。
 * 以下通用入口对受保护区域<b>默认拒绝</b>，不能因为调用者同租户就放行：
 * <ul>
 *   <li>{@code GET /files/{id}} 的公开 URL、{@code GET /files/{id}/download-url}、{@code GET /files/{id}/download}</li>
 *   <li>{@code DELETE /files/{id}}（防止绕过教材 A3 的快照引用检查）</li>
 *   <li>通用 presignedDownloadUrl / resolveExportFetchUrl</li>
 *   <li>{@code /exam/files/**} 的本地读盘与 remote fallback（含 nginx/CDN 等价路径）</li>
 * </ul>
 *
 * <p>识别必须基于 URL 解码并规范化后的路径，同时拒绝 {@code %2F}、反斜杠、{@code ..}、
 * 重复编码等变形，不能只对原始请求串做 {@code startsWith}。拒绝统一抛 404，不向探测方
 * 暴露该文件存在的事实；业务受控入口（如教材 A2）经自身授权后走 file 模块内部读取，
 * 不经过本闸门。
 */
public final class ProtectedFileAccessGuard {

  public static final String PROTECTED_PREFIX = "protected/";
  private static final int MAX_DECODE_ROUNDS = 3;

  private ProtectedFileAccessGuard() {
  }

  /** objectKey 是否落在受保护区域（objectKey 本身已是存储层规范形式，仅做反斜杠/点号防御）。 */
  public static boolean isProtectedObjectKey(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return false;
    }
    String normalized = objectKey.replace('\\', '/').trim();
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    return normalized.startsWith(PROTECTED_PREFIX);
  }

  /**
   * 请求路径（如 /exam/files/ 后的相对段）是否指向受保护区域。
   *
   * <p>先按未解码原文判断一次（拦 %2F 等编码变形），再循环解码到稳定（拦重复编码），
   * 全程拒绝反斜杠、{@code ..}、NUL 与非法转义。任何无法解析的路径按"可疑"处理，
   * 视为受保护区域请求（拒绝比放行安全）。
   */
  public static boolean isProtectedRequestPath(String rawPath) {
    if (rawPath == null || rawPath.isBlank()) {
      return false;
    }
    if (containsSuspiciousTokens(rawPath)) {
      return true;
    }
    String current = rawPath;
    for (int round = 0; round < MAX_DECODE_ROUNDS; round++) {
      String decoded;
      try {
        decoded = URLDecoder.decode(current, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException ex) {
        // 非法百分号转义：按可疑处理
        return true;
      }
      if (decoded.equals(current)) {
        break;
      }
      if (containsSuspiciousTokens(decoded)) {
        return true;
      }
      current = decoded;
    }
    String normalized = current.replace('\\', '/').trim();
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    // 大小写不敏感前缀 + 目录分隔变形（protected%2F 解码后即 protected/）
    return normalized.toLowerCase().startsWith(PROTECTED_PREFIX);
  }

  private static boolean containsSuspiciousTokens(String path) {
    if (path.indexOf('\0') >= 0) {
      return true;
    }
    String lower = path.toLowerCase();
    // 未解码的编码点（%2e=., %2f=/, %5c=\）一律视为变形尝试
    if (lower.contains("%2e") || lower.contains("%2f") || lower.contains("%5c")) {
      return true;
    }
    String normalized = path.replace('\\', '/');
    return normalized.contains("../") || normalized.contains("/..")
        || normalized.equals("..") || normalized.contains("./") || normalized.equals(".");
  }

  /** 通用外部入口（GET URL/download-url/download、DELETE、预签名）对受保护文件一律 404。 */
  public static void rejectExternalAccess(FileEntity entity) {
    if (entity != null && isProtectedObjectKey(entity.getObjectKey())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "File not found");
    }
  }

  /** 受保护文件在元数据类 VO 中 url 必须为 null（不暴露 objectKey/COS/公开 URL）。 */
  public static String externalUrlOrNull(FileEntity entity, String candidateUrl) {
    if (entity == null) {
      return candidateUrl;
    }
    return isProtectedObjectKey(entity.getObjectKey()) ? null : candidateUrl;
  }
}
