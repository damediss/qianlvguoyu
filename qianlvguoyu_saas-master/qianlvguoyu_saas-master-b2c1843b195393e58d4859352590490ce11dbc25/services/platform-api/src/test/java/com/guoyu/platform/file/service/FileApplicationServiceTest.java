package com.guoyu.platform.file.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.mapper.FileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

class FileApplicationServiceTest {

  private final FileApplicationService service = new FileApplicationService(
      Mockito.mock(FileMapper.class),
      new FileStorageProperties("local", "./target/test-files")
  );

  @Test
  void uploadRejectsEmptyFile() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.upload(file, "private"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("File is empty");
  }
}
