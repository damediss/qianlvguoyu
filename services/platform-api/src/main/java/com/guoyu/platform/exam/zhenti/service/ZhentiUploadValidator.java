package com.guoyu.platform.exam.zhenti.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 上传校验（方案 §2.4）：扩展名 / magic number / 大小 / 恶意 PDF 关键词。
 *
 * <p>校验顺序固定：扩展名(415) → 空(400) → 大小(413) → magic(415) → 恶意关键词(仅告警)。
 * 大小上限按字节参数暴露，便于单测覆盖边界而不必真的分配 50MB 内存。
 */
@Component
@Slf4j
public class ZhentiUploadValidator {

  /** 单 PDF 上限 50MB（代码层强制；全局 multipart 允许更大以承载 zip）。 */
  public static final long MAX_PDF_BYTES = 50L * 1024 * 1024;
  /** zip 上限 200MB。 */
  public static final long MAX_ZIP_BYTES = 200L * 1024 * 1024;
  /** zip 条目上限 100。 */
  public static final int MAX_ZIP_ENTRIES = 100;

  private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};
  private static final List<byte[]> SUSPICIOUS_PDF_TOKENS = List.of(
      "/JavaScript".getBytes(StandardCharsets.US_ASCII),
      "/Launch".getBytes(StandardCharsets.US_ASCII),
      "/EmbeddedFile".getBytes(StandardCharsets.US_ASCII)
  );

  public boolean isPdfName(String fileName) {
    return StringUtils.hasText(fileName) && fileName.toLowerCase().endsWith(".pdf");
  }

  public boolean isZipName(String fileName) {
    return StringUtils.hasText(fileName) && fileName.toLowerCase().endsWith(".zip");
  }

  /** 扩展名不合规 → 415。 */
  public void assertPdfExtension(String fileName) {
    if (!isPdfName(fileName)) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "仅支持 PDF 文件");
    }
  }

  /** 数量大小校验：0 字节 400、超限 413。 */
  public void assertPdfSize(long sizeBytes) {
    if (sizeBytes <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件为空，请重新选择");
    }
    if (sizeBytes > MAX_PDF_BYTES) {
      throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "单个 PDF 不能超过 50MB");
    }
  }

  /** 首 5 字节必须为 %PDF-，否则 415。 */
  public void assertPdfMagic(byte[] content) {
    if (content == null || content.length < PDF_MAGIC.length) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "文件内容不是有效的 PDF");
    }
    for (int i = 0; i < PDF_MAGIC.length; i++) {
      if (content[i] != PDF_MAGIC[i]) {
        throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "文件内容不是有效的 PDF");
      }
    }
  }

  /** 完整校验（不读库，供上传与批量导入共用）。 */
  public void validatePdf(String fileName, byte[] content) {
    assertPdfExtension(fileName);
    assertPdfSize(content == null ? 0 : content.length);
    assertPdfMagic(content);
    warnIfSuspicious(fileName, content);
  }

  /** 恶意 PDF 关键词：一期不拦截，仅 WARN 留观测（方案 §2.4）。 */
  public void warnIfSuspicious(String fileName, byte[] content) {
    List<String> hits = suspiciousTokens(content);
    if (!hits.isEmpty()) {
      log.warn("[ZhentiUpload] 可疑 PDF 关键词命中 file={} tokens={}（一期仅观测不拦截）",
          fileName, hits);
    }
  }

  public List<String> suspiciousTokens(byte[] content) {
    if (content == null || content.length == 0) {
      return List.of();
    }
    return SUSPICIOUS_PDF_TOKENS.stream()
        .filter(token -> indexOf(content, token) >= 0)
        .map(token -> new String(token, StandardCharsets.US_ASCII))
        .toList();
  }

  /** 文件 MD5（小写十六进制），去重键。 */
  public String md5(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("MD5 不可用", ex);
    }
  }

  private static int indexOf(byte[] haystack, byte[] needle) {
    outer:
    for (int i = 0; i <= haystack.length - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }
}
