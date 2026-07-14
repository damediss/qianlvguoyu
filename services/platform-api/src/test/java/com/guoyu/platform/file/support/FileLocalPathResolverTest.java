package com.guoyu.platform.file.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.file.config.FileStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLocalPathResolverTest {

  @TempDir
  Path tempDir;

  @Test
  void resolveExistingFindsFileUnderConfiguredRoot() throws Exception {
    Path root = tempDir.resolve("files");
    Files.createDirectories(root.resolve("1/2026/07"));
    Path file = root.resolve("1/2026/07/demo.docx");
    Files.writeString(file, "demo");

    FileLocalPathResolver resolver = new FileLocalPathResolver(
        new FileStorageProperties("local", root.toString(), null));
    assertThat(resolver.resolveExisting("1/2026/07/demo.docx")).contains(file);
  }
}
