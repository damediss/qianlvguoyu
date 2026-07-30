package com.guoyu.platform.trainingdepartment.support;

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
    byte[] head = new byte[4];
    try (InputStream in = Files.newInputStream(path)) {
      if (in.read(head) < 4) {
        return Kind.UNKNOWN;
      }
    }
    if ((head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF) {
      return Kind.LEGACY_EXCEL;
    }
    if (head[0] != 'P' || head[1] != 'K') {
      return Kind.UNKNOWN;
    }
    try (InputStream in = Files.newInputStream(path);
        ZipInputStream zip = new ZipInputStream(in)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (name.startsWith("xl/")) {
          return Kind.EXCEL;
        }
        if (name.startsWith("word/")) {
          return Kind.WORD;
        }
      }
    }
    return Kind.UNKNOWN;
  }
}
