package com.guoyu.platform.file.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** 上传后校验常见 Office / 图片文件魔数，避免损坏文件进入业务链路。 */
public final class StoredFileFormatValidator {

  private StoredFileFormatValidator() {}

  public static void assertSupportedOfficeFile(String filename, byte[] content) {
    if (filename == null || content == null || content.length == 0) {
      return;
    }
    String lower = filename.toLowerCase();
    if (!lower.endsWith(".xlsx")
        && !lower.endsWith(".xls")
        && !lower.endsWith(".docx")
        && !lower.endsWith(".doc")) {
      return;
    }
    if (!isOfficeMagic(content)) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST,
          "上传的文件不是有效的 Excel/Word 文档，请在本机用 Excel/WPS 打开确认无误后重新上传");
    }
  }

  public static void assertSupportedOfficeFile(String filename, Path path) throws IOException {
    if (filename == null || path == null || !Files.exists(path)) {
      return;
    }
    byte[] head = new byte[16];
    try (InputStream in = Files.newInputStream(path)) {
      int read = in.read(head);
      if (read <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "上传的文件为空");
      }
      if (read < head.length) {
        byte[] trimmed = new byte[read];
        System.arraycopy(head, 0, trimmed, 0, read);
        head = trimmed;
      }
      assertSupportedOfficeFile(filename, head);
      // Path 版本拿不到 Multipart Content-Type，按扩展名识别图片
      assertSupportedImageFile(filename, null, head);
    }
  }

  /** 按扩展名或 Content-Type 校验图片魔数，拦截「扩展名是 .png 但内容损坏」的上传。 */
  public static void assertSupportedImageFile(String filename, String contentType, byte[] head) {
    if (head == null || head.length == 0) {
      return;
    }
    if (!looksLikeImage(filename, contentType)) {
      return;
    }
    if (!isImageMagic(head)) {
      throw new BusinessException(
          ErrorCode.BAD_REQUEST,
          "上传的文件不是有效图片（内容已损坏或格式不支持），请用 JPG/PNG 重新导出后再上传");
    }
  }

  static boolean looksLikeImage(String filename, String contentType) {
    if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
      return true;
    }
    if (filename == null) {
      return false;
    }
    String lower = filename.toLowerCase();
    return lower.endsWith(".png")
        || lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".gif")
        || lower.endsWith(".bmp")
        || lower.endsWith(".webp");
  }

  static boolean isImageMagic(byte[] head) {
    if (head.length < 3) {
      return false;
    }
    // PNG
    if (head.length >= 8
        && (head[0] & 0xFF) == 0x89
        && head[1] == 'P'
        && head[2] == 'N'
        && head[3] == 'G') {
      return true;
    }
    // JPEG
    if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
      return true;
    }
    // GIF
    if (head.length >= 6
        && head[0] == 'G'
        && head[1] == 'I'
        && head[2] == 'F'
        && head[3] == '8'
        && (head[4] == '7' || head[4] == '9')
        && head[5] == 'a') {
      return true;
    }
    // BMP
    if (head[0] == 'B' && head[1] == 'M') {
      return true;
    }
    // WEBP (RIFF....WEBP)
    return head.length >= 12
        && head[0] == 'R'
        && head[1] == 'I'
        && head[2] == 'F'
        && head[3] == 'F'
        && head[8] == 'W'
        && head[9] == 'E'
        && head[10] == 'B'
        && head[11] == 'P';
  }

  static boolean isOfficeMagic(byte[] head) {
    if (head.length < 4) {
      return false;
    }
    boolean xlsxOrDocx = head[0] == 'P' && head[1] == 'K';
    boolean xlsOrDoc = (head[0] & 0xFF) == 0xD0
        && (head[1] & 0xFF) == 0xCF
        && (head[2] & 0xFF) == 0x11
        && (head[3] & 0xFF) == 0xE0;
    return xlsxOrDocx || xlsOrDoc;
  }
}
