package com.guoyu.platform.certification.collection.vo;

import java.time.LocalDateTime;

public record SignatureRecordVO(
    Long id,
    Long taskId,
    Long candidateId,
    Long sessionId,
    Long signatureFileId,
    String signatureHash,
    String signerType,
    String signerTypeLabel,
    String signerName,
    LocalDateTime signedAt,
    String recordStatus,
    String recordStatusLabel
) {
}
