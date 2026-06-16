package com.guoyu.platform.file.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** 上传后校验常见 Office 文件魔数，避免损坏文件进入业务链路。 */
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
    byte[] head = new byte[8];
    try (InputStream in = Files.newInputStream(path)) {
      int read = in.read(head);
      if (read <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "上传的文件为空");
      }
      assertSupportedOfficeFile(filename, head);
    }
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
