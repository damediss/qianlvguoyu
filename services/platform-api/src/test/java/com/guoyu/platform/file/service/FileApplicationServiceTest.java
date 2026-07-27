package com.guoyu.platform.file.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.file.FileStorage;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.file.support.FileLocalPathResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

  @Test
  void defersPhysicalDeleteUntilDatabaseCommit() {
    FileEntity entity = new FileEntity();
    entity.setId(5001L);
    entity.setTenantId(1L);
    entity.setObjectKey("1/rules.pdf");
    entity.setStatus("uploaded");
    entity.setDeleted(0);
    when(mapper.selectOne(any())).thenReturn(entity);
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.deleteAfterCommitForTenant(1L, 5001L);

      verify(storage, never()).delete("1/rules.pdf");
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(storage).delete("1/rules.pdf");
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void removesNewStorageObjectWhenMetadataInsertFails() {
    when(storage.providerName()).thenReturn("local");
    when(storage.put(any(), any(), any(Long.class), any()))
        .thenReturn("/private/new-file");
    Mockito.doThrow(new IllegalStateException("insert failed"))
        .when(mapper).insert(any(FileEntity.class));
    MockMultipartFile file =
        new MockMultipartFile("file", "note.txt", "text/plain", "content".getBytes());

    assertThatThrownBy(() -> service.uploadForTenant(1L, file, "private"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("insert failed");
    verify(storage).delete(Mockito.argThat(key -> key.endsWith("-note.txt")));
  }

  @Test
  void removesNewStorageObjectWhenOuterTransactionRollsBack() {
    when(storage.providerName()).thenReturn("local");
    when(storage.put(any(), any(), any(Long.class), any()))
        .thenReturn("/private/new-file");
    MockMultipartFile file =
        new MockMultipartFile("file", "note.txt", "text/plain", "content".getBytes());
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.uploadForTenant(1L, file, "private");
      verify(storage, never()).delete(any());

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      verify(storage).delete(Mockito.argThat(key -> key.endsWith("-note.txt")));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}
