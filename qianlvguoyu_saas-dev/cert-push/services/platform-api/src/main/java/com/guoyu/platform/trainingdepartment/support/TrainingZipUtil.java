package com.guoyu.platform.trainingdepartment.support;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** CDN 目录/文件打包为 ZIP */
public final class TrainingZipUtil {

  private TrainingZipUtil() {}

  public static void zipEntries(Path zipFile, List<Path> entries) throws IOException {
    Files.createDirectories(zipFile.getParent());
    try (OutputStream out = Files.newOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(out)) {
      for (Path entry : entries) {
        if (Files.isDirectory(entry)) {
          zipDirectory(zos, entry, entry.getFileName().toString());
        } else if (Files.isRegularFile(entry)) {
          addFile(zos, entry, entry.getFileName().toString());
        }
      }
    }
  }

  private static void zipDirectory(ZipOutputStream zos, Path dir, String baseName) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path rel = dir.relativize(file);
        String entryName = baseName + "/" + rel.toString().replace("\\", "/");
        addFile(zos, file, entryName);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private static void addFile(ZipOutputStream zos, Path file, String entryName) throws IOException {
    ZipEntry entry = new ZipEntry(entryName);
    zos.putNextEntry(entry);
    Files.copy(file, zos);
    zos.closeEntry();
  }
}
