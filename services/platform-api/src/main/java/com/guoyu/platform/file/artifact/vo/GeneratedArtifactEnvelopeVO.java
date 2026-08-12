package com.guoyu.platform.file.artifact.vo;

import java.util.List;

public record GeneratedArtifactEnvelopeVO(
    String schema,
    String businessType,
    String businessId,
    String taskId,
    List<GeneratedArtifactRefVO> artifacts
) {
  public record GeneratedArtifactRefVO(
      String provider,
      String documentId,
      String fileId,
      String filename,
      String mimeType,
      long size,
      String sha256,
      int version,
      boolean draft
  ) {
  }
}
