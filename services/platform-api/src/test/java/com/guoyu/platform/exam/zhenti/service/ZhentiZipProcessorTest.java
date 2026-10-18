package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportFileResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZhentiZipProcessorTest {

  @Mock
  private ZhentiIngestService ingestService;

  private ZhentiZipProcessor processor;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    processor = new ZhentiZipProcessor(ingestService);
  }

  private Path zip(String... entryNames) throws IOException {
    Path path = tempDir.resolve("test-" + System.nanoTime() + ".zip");
    try (OutputStream out = Files.newOutputStream(path);
        ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
      for (String name : entryNames) {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(("%PDF-1.4\n" + name).getBytes(StandardCharsets.US_ASCII));
        zip.closeEntry();
      }
    }
    return path;
  }

  @Test
  @DisplayName("正常 zip：登记全部非目录条目")
  void inspectCountsEntries() throws IOException {
    Path path = zip("a.pdf", "b.pdf");
    var inspection = processor.inspect(path);
    assertThat(inspection.total()).isEqualTo(2);
  }

  @Test
  @DisplayName("zip-slip（../evil.pdf）→ 400")
  void zipSlipRejected() throws IOException {
    Path path = zip("../evil.pdf");
    assertThatThrownBy(() -> processor.inspect(path))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("400"));
  }

  @Test
  @DisplayName("绝对路径条目 → 400")
  void absolutePathRejected() throws IOException {
    Path path = zip("/etc/passwd.pdf");
    assertThatThrownBy(() -> processor.inspect(path))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("400"));
  }

  @Test
  @DisplayName("101 个条目 → 413")
  void tooManyEntriesRejected() throws IOException {
    String[] names = new String[ZhentiUploadValidator.MAX_ZIP_ENTRIES + 1];
    for (int i = 0; i < names.length; i++) {
      names[i] = "file-" + i + ".pdf";
    }
    Path path = zip(names);
    assertThatThrownBy(() -> processor.inspect(path))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("413"));
  }

  @Test
  @DisplayName("空 zip → 400")
  void emptyZipRejected() throws IOException {
    Path path = zip();
    assertThatThrownBy(() -> processor.inspect(path))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("400"));
  }

  @Test
  @DisplayName("zip 内 .exe 计失败明细，不中断整包")
  void nonPdfCountedAsFailure() throws IOException {
    Path path = zip("good-2024.pdf", "bad.exe");
    ExamZhentiEntity entity = new ExamZhentiEntity();
    entity.setId(1L);
    entity.setTitle("good");
    entity.setYear("2024");
    entity.setMetaPending(0);
    when(ingestService.ingest(anyLong(), anyLong(), any(), anyString(), any(), any(), any()))
        .thenReturn(entity);

    List<ZhentiImportFileResult> results = processor.process(
        path, 10159L, 100L, 1L, null);

    assertThat(results).hasSize(2);
    assertThat(results.get(0).ok()).isTrue();
    assertThat(results.get(0).fileName()).isEqualTo("good-2024.pdf");
    assertThat(results.get(1).ok()).isFalse();
    assertThat(results.get(1).reason()).contains("文件类型非法");
  }

  @Test
  @DisplayName("文件名解析不出元数据 → 标记待补录且带回记录 id")
  void pendingEntryCarriesId() throws IOException {
    Path path = zip("no-year-name.pdf");
    ExamZhentiEntity entity = new ExamZhentiEntity();
    entity.setId(7L);
    entity.setTitle("no-year-name");
    entity.setYear(null);
    entity.setMetaPending(1);
    when(ingestService.ingest(anyLong(), anyLong(), any(), anyString(), any(), any(), any()))
        .thenReturn(entity);

    List<ZhentiImportFileResult> results = processor.process(path, 10159L, 100L, 1L, null);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).ok()).isFalse();
    assertThat(results.get(0).pending()).isTrue();
    assertThat(results.get(0).id()).isEqualTo("7");
  }
}
