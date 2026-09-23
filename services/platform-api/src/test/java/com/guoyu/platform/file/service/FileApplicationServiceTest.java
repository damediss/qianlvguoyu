package com.guoyu.platform.file.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.file.FileStorage;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import com.guoyu.platform.file.support.FileLocalPathResolver;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class FileApplicationServiceTest {

  private final FileMapper mapper = Mockito.mock(FileMapper.class);
  private final FileStorage storage = Mockito.mock(FileStorage.class);
  private final FileStorageProperties properties = new FileStorageProperties("local", "./target/test-files", "dev",
      new FileStorageProperties.Cos(null, null, null, null, null));
  private final FileApplicationService service = new FileApplicationService(
      mapper,
      properties,
      storage,
      new FileLocalPathResolver(properties)
  );

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private static void loginAs(long tenantId) {
    // P0-5 收紧后 upload() 先经 getTenantIdSafe 取当前用户（未登录 401），空文件校验在其后
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        new PlatformUserPrincipal(1L, tenantId, "tester", "测试用户", "admin", List.of(), List.of(), null),
        "token", List.of()));
  }

  @Test
  void uploadRejectsEmptyFile() {
    loginAs(1L);
    MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
    assertThatThrownBy(() -> service.upload(file, "private"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("File is empty");
  }

  @Test
  void storeFromBytesRejectsEmptyFile() {
    loginAs(1L);
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
    verify(storage).delete(Mockito.argThat(key -> key.startsWith("dev/1/") && key.endsWith(".txt")));
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
      verify(storage).delete(Mockito.argThat(key -> key.startsWith("dev/1/") && key.endsWith(".txt")));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void prefixesNewObjectKeysWithConfiguredEnvironment() {
    when(storage.providerName()).thenReturn("cos");
    when(storage.put(any(), any(), any(Long.class), any())).thenReturn("https://example.invalid/file");
    MockMultipartFile textFile =
        new MockMultipartFile("file", "note.txt", "text/plain", "content".getBytes());
    service.uploadForTenant(1L, textFile, "private");

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(storage).put(keyCaptor.capture(), any(), any(Long.class), any());
    assertThat(keyCaptor.getValue())
        .startsWith("dev/1/")
        .endsWith(".txt");
  }

  @Test
  void downloadUrlReturnsPresignedUrlForCosFile() throws Exception {
    authAsTenant(1L);
    FileEntity entity = uploadedFile(7001L, 1L, "cos");
    when(mapper.selectOne(any())).thenReturn(entity);
    java.net.URL presigned = new java.net.URL(
        "https://bucket.cos.ap-guangzhou.myqcloud.com/dev/1/pack.zip?sign=xyz");
    when(storage.presignedDownloadUrl(eq("dev/1/pack.zip"), any(java.time.Duration.class)))
        .thenReturn(java.util.Optional.of(presigned));

    var vo = service.downloadUrl(7001L);

    assertThat(vo.url()).isEqualTo(presigned.toString());
    assertThat(vo.accessPolicy()).isEqualTo("private");
  }

  @Test
  void downloadUrlFallsBackToProxyForLocalFile() {
    authAsTenant(1L);
    FileEntity entity = uploadedFile(7002L, 1L, "local");
    when(mapper.selectOne(any())).thenReturn(entity);

    var vo = service.downloadUrl(7002L);

    // local 无法直链：mock 的默认空 Optional → 回退鉴权中转路径
    assertThat(vo.url()).isEqualTo("/api/files/7002/download");
  }

  private void authAsTenant(Long tenantId) {
    com.guoyu.platform.security.PlatformUserPrincipal principal =
        new com.guoyu.platform.security.PlatformUserPrincipal(
            1L, tenantId, "tester", "测试", "platform",
            java.util.List.of(), java.util.List.of(), "a@b.c");
    org.springframework.security.core.context.SecurityContextHolder.getContext()
        .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            principal, "token", principal.getAuthorities()));
  }

  private FileEntity uploadedFile(Long id, Long tenantId, String provider) {
    FileEntity entity = new FileEntity();
    entity.setId(id);
    entity.setTenantId(tenantId);
    entity.setObjectKey("dev/1/pack.zip");
    entity.setStorageProvider(provider);
    entity.setAccessPolicy("private");
    entity.setStatus("uploaded");
    entity.setDeleted(0);
    return entity;
  }
}
