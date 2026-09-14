package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SignUpDefaultTemplateExportPathsTest {

  @Test
  void taskZipRelative_usesTaskSuffix() {
    assertThat(SignUpDefaultTemplateExportPaths.taskZipRelative("2026测试班", 259L))
        .isEqualTo("down/2026测试班_t259.zip");
  }

  @Test
  void sanitizeTitle_replacesIllegalChars() {
    assertThat(SignUpDefaultTemplateExportPaths.sanitizeTitle(" a/b:c*?\"<>| "))
        .isEqualTo("a_b_c______");
  }

  @Test
  void endsWithTaskZip_matchesOnlyCurrentTask() {
    assertThat(SignUpDefaultTemplateExportPaths.endsWithTaskZip("down/x_t259.zip", 259L)).isTrue();
    assertThat(SignUpDefaultTemplateExportPaths.endsWithTaskZip("down/x_t258.zip", 259L)).isFalse();
  }

  @Test
  void zipAbsolute_goesToDownSubdirNotCdnRoot(@TempDir Path cdn) throws Exception {
    Path zip = SignUpDefaultTemplateExportPaths.zipAbsolute(
        cdn.toString(), "down/2026强强碳素自费认定中级班-资料收集_t269.zip");
    Files.createDirectories(zip.getParent());
    Files.writeString(zip, "ok", StandardCharsets.UTF_8);

    assertThat(zip.getParent().getFileName().toString()).isEqualTo("down");
    assertThat(zip).exists().isRegularFile();
    assertThat(cdn.resolve("down/2026强强碳素自费认定中级班-资料收集_t269.zip").toAbsolutePath().normalize())
        .isEqualTo(zip.toAbsolutePath().normalize());
  }

  @Test
  void findZipByTaskId_scansDownDir(@TempDir Path cdn) throws Exception {
    Path zip = SignUpDefaultTemplateExportPaths.zipAbsolute(
        cdn.toString(), "2026强强碳素自费认定中级班-资料收集_t269.zip");
    Files.createDirectories(zip.getParent());
    Files.writeString(zip, "ok", StandardCharsets.UTF_8);

    Path found = SignUpDefaultTemplateExportPaths.findZipByTaskId(cdn.toString(), 269L);
    assertThat(found).isNotNull().exists();
    assertThat(found.getFileName().toString()).endsWith("_t269.zip");
    assertThat(SignUpDefaultTemplateExportPaths.findZipByTaskId(cdn.toString(), 270L)).isNull();
  }
}
