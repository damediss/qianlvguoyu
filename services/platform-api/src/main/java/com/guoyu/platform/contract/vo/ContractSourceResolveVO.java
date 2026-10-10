package com.guoyu.platform.contract.vo;

import java.util.List;
import java.util.Map;

public record ContractSourceResolveVO(
    String sourceSnapshotId,
    String knowledgeBaseId,
    String knowledgeId,
    String originalFilename,
    String sourceFileHash,
    String manifestId,
    int manifestVersion,
    String derivationStatus,
    Map<String, String> detectedFields,
    List<AllowedAnchorVO> allowedAnchors,
    List<String> warnings
) {
  public record AllowedAnchorVO(
      String anchorId,
      String field,
      List<String> allowedOperations
  ) {}
}
