package com.guoyu.platform.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.contract.entity.ContractSourceManifestEntity;
import com.guoyu.platform.contract.entity.ContractSourceSnapshotEntity;
import com.guoyu.platform.contract.infrastructure.WeKnoraContractSourceClient.KnowledgeSourceMetadata;
import com.guoyu.platform.contract.mapper.ContractSourceManifestMapper;
import com.guoyu.platform.contract.mapper.ContractSourceSnapshotMapper;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists the source file, snapshot row, and manifest in one transaction. */
@Service
public class ContractSourceSnapshotStore {

  public static final String DOCX_MIME =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

  private final ContractSourceSnapshotMapper snapshotMapper;
  private final ContractSourceManifestMapper manifestMapper;
  private final FileApplicationService fileApplicationService;

  public ContractSourceSnapshotStore(
      ContractSourceSnapshotMapper snapshotMapper,
      ContractSourceManifestMapper manifestMapper,
      FileApplicationService fileApplicationService) {
    this.snapshotMapper = snapshotMapper;
    this.manifestMapper = manifestMapper;
    this.fileApplicationService = fileApplicationService;
  }

  @Transactional
  public StoredSource store(
      Long tenantId,
      KnowledgeSourceMetadata metadata,
      byte[] sourceBytes,
      String sourceSha256,
      ContractDocxDerivationService.AnalysisResult analysis,
      String manifestJson) {
    ContractSourceSnapshotEntity existing = snapshotMapper.selectOne(
        new LambdaQueryWrapper<ContractSourceSnapshotEntity>()
            .eq(ContractSourceSnapshotEntity::getTenantId, tenantId)
            .eq(ContractSourceSnapshotEntity::getProvider, "weknora-knowledge")
            .eq(ContractSourceSnapshotEntity::getKnowledgeId, metadata.knowledgeId())
            .eq(ContractSourceSnapshotEntity::getSourceFileHash, sourceSha256)
            .eq(ContractSourceSnapshotEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (existing != null) {
      ContractSourceManifestEntity existingManifest = manifestMapper.selectOne(
          new LambdaQueryWrapper<ContractSourceManifestEntity>()
              .eq(ContractSourceManifestEntity::getTenantId, tenantId)
              .eq(ContractSourceManifestEntity::getSourceSnapshotId, existing.getId())
              .eq(ContractSourceManifestEntity::getDeleted, 0)
              .orderByDesc(ContractSourceManifestEntity::getVersion)
              .last("LIMIT 1"));
      if (existingManifest != null
          && ContractDocxDerivationService.RENDERER_VERSION.equals(
              existingManifest.getRendererVersion())
          && "active".equals(existingManifest.getStatus())) {
        return new StoredSource(existing, existingManifest);
      }
      existing.setDerivationStatus(analysis.derivationStatus());
      existing.setSourceParseStatus(metadata.parseStatus());
      existing.setSourceEnableStatus(metadata.enableStatus());
      snapshotMapper.updateById(existing);
      ContractSourceManifestEntity nextManifest = newManifest(
          tenantId,
          existing.getId(),
          existingManifest == null ? 1 : existingManifest.getVersion() + 1,
          analysis,
          manifestJson);
      manifestMapper.insert(nextManifest);
      return new StoredSource(existing, nextManifest);
    }

    String snapshotFilename = "source-snapshot-" + metadata.knowledgeId() + ".docx";
    FileVO stored = fileApplicationService.storeFromBytesForTenant(
        tenantId, sourceBytes, snapshotFilename, DOCX_MIME, "private");
    FileEntity storedFile = fileApplicationService.getExistingFile(tenantId, stored.id());

    ContractSourceSnapshotEntity snapshot = new ContractSourceSnapshotEntity();
    snapshot.setTenantId(tenantId);
    snapshot.setProvider("weknora-knowledge");
    snapshot.setKnowledgeBaseId(metadata.knowledgeBaseId());
    snapshot.setKnowledgeId(metadata.knowledgeId());
    snapshot.setSourceFileId(storedFile.getId());
    snapshot.setOriginalFilename(metadata.filename());
    snapshot.setSourceMimeType(DOCX_MIME);
    snapshot.setSourceFileHash(sourceSha256);
    snapshot.setSourceFileSize((long) sourceBytes.length);
    snapshot.setDerivationStatus(analysis.derivationStatus());
    snapshot.setSourceParseStatus(metadata.parseStatus());
    snapshot.setSourceEnableStatus(metadata.enableStatus());
    snapshot.setDeleted(0);
    snapshotMapper.insert(snapshot);

    ContractSourceManifestEntity manifest = newManifest(
        tenantId, snapshot.getId(), 1, analysis, manifestJson);
    manifestMapper.insert(manifest);
    return new StoredSource(snapshot, manifest);
  }

  private ContractSourceManifestEntity newManifest(
      Long tenantId,
      Long sourceSnapshotId,
      int version,
      ContractDocxDerivationService.AnalysisResult analysis,
      String manifestJson) {
    ContractSourceManifestEntity manifest = new ContractSourceManifestEntity();
    manifest.setTenantId(tenantId);
    manifest.setSourceSnapshotId(sourceSnapshotId);
    manifest.setVersion(version);
    manifest.setManifestJson(manifestJson);
    manifest.setAnchorCount(analysis.manifest().anchors().size());
    manifest.setWarningCount(analysis.manifest().warnings().size());
    manifest.setStatus("active");
    manifest.setRendererVersion(ContractDocxDerivationService.RENDERER_VERSION);
    manifest.setDeleted(0);
    return manifest;
  }

  public record StoredSource(
      ContractSourceSnapshotEntity snapshot,
      ContractSourceManifestEntity manifest
  ) {}
}
