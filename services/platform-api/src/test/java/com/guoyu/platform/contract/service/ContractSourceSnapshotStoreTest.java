package com.guoyu.platform.contract.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.contract.entity.ContractSourceManifestEntity;
import com.guoyu.platform.contract.entity.ContractSourceSnapshotEntity;
import com.guoyu.platform.contract.infrastructure.WeKnoraContractSourceClient.KnowledgeSourceMetadata;
import com.guoyu.platform.contract.mapper.ContractSourceManifestMapper;
import com.guoyu.platform.contract.mapper.ContractSourceSnapshotMapper;
import com.guoyu.platform.file.service.FileApplicationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ContractSourceSnapshotStoreTest {

  @Test
  void createsNextManifestVersionWithoutDuplicatingSnapshotFile() {
    ContractSourceSnapshotMapper snapshotMapper = mock(ContractSourceSnapshotMapper.class);
    ContractSourceManifestMapper manifestMapper = mock(ContractSourceManifestMapper.class);
    FileApplicationService fileService = mock(FileApplicationService.class);
    ContractSourceSnapshotStore store =
        new ContractSourceSnapshotStore(snapshotMapper, manifestMapper, fileService);

    ContractSourceSnapshotEntity snapshot = new ContractSourceSnapshotEntity();
    snapshot.setId(100L);
    snapshot.setDerivationStatus("requires_review");
    ContractSourceManifestEntity oldManifest = new ContractSourceManifestEntity();
    oldManifest.setId(200L);
    oldManifest.setVersion(4);
    oldManifest.setRendererVersion("contract-docx-poc-0");
    oldManifest.setStatus("active");
    when(snapshotMapper.selectOne(any())).thenReturn(snapshot);
    when(manifestMapper.selectOne(any())).thenReturn(oldManifest);

    ContractDocxDerivationService.ContractSourceManifest content =
        new ContractDocxDerivationService.ContractSourceManifest(
            ContractDocxDerivationService.MANIFEST_SCHEMA,
            ContractDocxDerivationService.RENDERER_VERSION,
            Map.of("partyA", "甲方"),
            List.of(),
            List.of());
    ContractDocxDerivationService.AnalysisResult analysis =
        new ContractDocxDerivationService.AnalysisResult(content, "derivable");

    ContractSourceSnapshotStore.StoredSource result = store.store(
        1L,
        new KnowledgeSourceMetadata(
            "knowledge-1", "kb-1", "历史合同.docx", "docx", 3,
            "", "completed", "enabled"),
        new byte[] {1, 2, 3},
        "a".repeat(64),
        analysis,
        "{}");

    ArgumentCaptor<ContractSourceManifestEntity> manifestCaptor =
        ArgumentCaptor.forClass(ContractSourceManifestEntity.class);
    verify(manifestMapper).insert(manifestCaptor.capture());
    assertEquals(5, manifestCaptor.getValue().getVersion());
    assertEquals(100L, manifestCaptor.getValue().getSourceSnapshotId());
    assertEquals("derivable", snapshot.getDerivationStatus());
    assertSame(snapshot, result.snapshot());
    assertSame(manifestCaptor.getValue(), result.manifest());
    verify(fileService, never()).storeFromBytesForTenant(any(), any(), any(), any(), any());
  }
}
