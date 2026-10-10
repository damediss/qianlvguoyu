package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 识别 OOXML（.docx/.xlsx 等 ZIP 容器）内的实际文档类型。 */
public final class TrainingOfficeFileKind {

  public enum Kind {
    EXCEL,
    WORD,
    LEGACY_EXCEL,
    UNKNOWN
  }

  private TrainingOfficeFileKind() {}

  public static Kind detect(Path path) throws IOException {
    byte[] head = new byte[8];
    int n;
    try (InputStream in = Files.newInputStream(path)) {
      n = in.read(head);
    }
    if (n < 4) {
      return Kind.UNKNOWN;
    }
    Kind fromHead = kindFromMagic(head);
    if (fromHead != Kind.EXCEL) {
      return fromHead;
    }
    try (InputStream in = Files.newInputStream(path)) {
      return kindFromZipEntries(in);
    }
  }

  /** 按文件魔数 + ZIP 内路径区分 docx / xlsx / 旧版 xls。 */
  public static Kind detect(byte[] bytes) {
    if (bytes == null || bytes.length < 4) {
      return Kind.UNKNOWN;
    }
    Kind fromHead = kindFromMagic(bytes);
    if (fromHead != Kind.EXCEL) {
      return fromHead;
    }
    try {
      return kindFromZipEntries(new ByteArrayInputStream(bytes));
    } catch (IOException ex) {
      return Kind.UNKNOWN;
    }
  }

  /** 填数/存储用格式串：docx | xlsx | xls | 空。 */
  public static String toFormat(Kind kind) {
    if (kind == null) {
      return "";
    }
    return switch (kind) {
      case WORD -> "docx";
      case EXCEL -> "xlsx";
      case LEGACY_EXCEL -> "xls";
      case UNKNOWN -> "";
    };
  }

  public static String sniffFormat(byte[] bytes) {
    return toFormat(detect(bytes));
  }

  private static Kind kindFromMagic(byte[] head) {
    if ((head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF) {
      return Kind.LEGACY_EXCEL;
    }
    if (head[0] == 'P' && head[1] == 'K') {
      // ZIP：需看 word/ 或 xl/ 再定；此处用 EXCEL 作「待扫 ZIP」哨兵
      return Kind.EXCEL;
    }
    return Kind.UNKNOWN;
  }

  private static Kind kindFromZipEntries(InputStream in) throws IOException {
    try (ZipInputStream zip = new ZipInputStream(in)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (name == null) {
          continue;
        }
        String norm = name.replace('\\', '/');
        if (norm.startsWith("xl/")) {
          return Kind.EXCEL;
        }
        if (norm.startsWith("word/")) {
          return Kind.WORD;
        }
      }
    }
    return Kind.UNKNOWN;
  }
}
