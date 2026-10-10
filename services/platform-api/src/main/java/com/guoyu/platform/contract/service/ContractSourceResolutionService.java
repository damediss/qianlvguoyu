package com.guoyu.platform.contract.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.contract.dto.ContractSourceResolveReq;
import com.guoyu.platform.contract.infrastructure.WeKnoraContractSourceClient;
import com.guoyu.platform.contract.infrastructure.WeKnoraContractSourceClient.KnowledgeSourceMetadata;
import com.guoyu.platform.contract.vo.ContractSourceResolveVO;
import com.guoyu.platform.integration.bot.BotActorContext;
import com.guoyu.platform.integration.bot.BotActorContextHolder;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContractSourceResolutionService {

  private final WeKnoraContractSourceClient sourceClient;
  private final ContractDocxDerivationService docxDerivationService;
  private final ContractSourceSnapshotStore snapshotStore;

  public ContractSourceResolutionService(
      WeKnoraContractSourceClient sourceClient,
      ContractDocxDerivationService docxDerivationService,
      ContractSourceSnapshotStore snapshotStore) {
    this.sourceClient = sourceClient;
    this.docxDerivationService = docxDerivationService;
    this.snapshotStore = snapshotStore;
  }

  public ContractSourceResolveVO resolve(ContractSourceResolveReq request) {
    BotActorContext actor = requirePrivateActor();
    KnowledgeSourceMetadata metadata = sourceClient.getMetadata(request.knowledgeId().trim());
    validateMetadata(request, metadata);

    byte[] sourceBytes = sourceClient.downloadOriginal(metadata.knowledgeId());
    if (metadata.fileSize() > 0 && metadata.fileSize() != sourceBytes.length) {
      throw new BusinessException(ErrorCode.CONFLICT, "WeKnora source size changed during resolution");
    }
    validateProviderHash(metadata.fileHash(), sourceBytes);
    String sourceSha256 = digest("SHA-256", sourceBytes);

    ContractDocxDerivationService.AnalysisResult analysis = docxDerivationService.analyze(sourceBytes);
    String manifestJson = docxDerivationService.writeManifest(analysis.manifest());
    ContractSourceSnapshotStore.StoredSource stored = snapshotStore.store(
        actor.tenantId(), metadata, sourceBytes, sourceSha256, analysis, manifestJson);

    var anchors = analysis.manifest().anchors().stream()
        .map(anchor -> new ContractSourceResolveVO.AllowedAnchorVO(
            anchor.anchorId(), anchor.field(), anchor.allowedOperations()))
        .toList();
    return new ContractSourceResolveVO(
        String.valueOf(stored.snapshot().getId()),
        stored.snapshot().getKnowledgeBaseId(),
        stored.snapshot().getKnowledgeId(),
        stored.snapshot().getOriginalFilename(),
        stored.snapshot().getSourceFileHash(),
        String.valueOf(stored.manifest().getId()),
        stored.manifest().getVersion(),
        stored.snapshot().getDerivationStatus(),
        analysis.manifest().detectedFields(),
        anchors,
        analysis.manifest().warnings()
    );
  }

  private void validateMetadata(ContractSourceResolveReq request, KnowledgeSourceMetadata metadata) {
    if (!request.knowledgeId().trim().equals(metadata.knowledgeId())
        || !request.knowledgeBaseId().trim().equals(metadata.knowledgeBaseId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Knowledge source does not belong to the selected knowledge base");
    }
    String filename = metadata.filename().toLowerCase(Locale.ROOT);
    if (!filename.endsWith(".docx") || !"docx".equals(metadata.fileType())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Only DOCX knowledge sources can be derived");
    }
    if (!"completed".equalsIgnoreCase(metadata.parseStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "Knowledge source parsing is not completed");
    }
    if (!"enabled".equalsIgnoreCase(metadata.enableStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "Knowledge source is not enabled");
    }
  }

  private void validateProviderHash(String providerHash, byte[] bytes) {
    if (!StringUtils.hasText(providerHash)) {
      return;
    }
    String normalized = providerHash.trim().toLowerCase(Locale.ROOT);
    String algorithm = normalized.length() == 32 ? "MD5" : normalized.length() == 64 ? "SHA-256" : null;
    if (algorithm == null || !digest(algorithm, bytes).equals(normalized)) {
      throw new BusinessException(ErrorCode.CONFLICT, "WeKnora source hash does not match downloaded content");
    }
  }

  private String digest(String algorithm, byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Source digest is unavailable");
    }
  }

  private BotActorContext requirePrivateActor() {
    BotActorContext actor = BotActorContextHolder.get();
    if (actor == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Verified bot actor context is required");
    }
    if (!actor.isPrivateChat()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "Contract sources can only be resolved for a private chat");
    }
    return actor;
  }
}
