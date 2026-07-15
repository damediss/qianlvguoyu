package com.guoyu.platform.file.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.file.FileStorage;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.file.support.FileLocalPathResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

class FileApplicationServiceTest {

  private final FileMapper mapper = Mockito.mock(FileMapper.class);
  private final FileStorage storage = Mockito.mock(FileStorage.class);
  private final FileStorageProperties properties = new FileStorageProperties("local", "./target/test-files",
      new FileStorageProperties.Cos(null, null, null, null, null));
  private final FileApplicationService service = new FileApplicationService(
      mapper,
      properties,
      storage,
      new FileLocalPathResolver(properties)
  );

  @Test
  void uploadRejectsEmptyFile() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
    assertThatThrownBy(() -> service.upload(file, "private"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("File is empty");
  }

  @Test
  void storeFromBytesRejectsEmptyFile() {
    assertThatThrownBy(() -> service.storeFromBytes(new byte[0], "x.bin", "application/octet-stream", "private"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("File is empty");
  }
}
