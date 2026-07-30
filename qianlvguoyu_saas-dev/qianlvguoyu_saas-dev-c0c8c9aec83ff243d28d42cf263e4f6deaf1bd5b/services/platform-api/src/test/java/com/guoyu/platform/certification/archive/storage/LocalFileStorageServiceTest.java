package com.guoyu.platform.certification.archive.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void storeWritesFileAndReturnsMetadata() throws Exception {
    LocalFileStorageService service = service();
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "../归档.pdf",
        "application/pdf",
        "archive".getBytes()
    );

    ArchiveFileObject object = service.store(file, "certification/archive/T001/1/task-materials/ID/file.pdf");

    assertThat(object.fileName()).doesNotContain("..").doesNotContain("/");
    assertThat(object.fileSize()).isEqualTo(7);
    assertThat(object.fileHash()).hasSize(64);
    assertThat(Files.exists(tempDir.resolve(object.fileKey()))).isTrue();
  }

  @Test
  void storeRejectsPathTraversalFileKey() {
    LocalFileStorageService service = service();
    MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

    assertThatThrownBy(() -> service.store(file, "../a.pdf"))
        .isInstanceOf(BusinessException.class);
  }

  private LocalFileStorageService service() {
    CertificationArchiveStorageProperties properties = new CertificationArchiveStorageProperties();
    properties.setStorageRoot(tempDir.toString());
    properties.setStorageType("local");
    return new LocalFileStorageService(properties);
  }
}
