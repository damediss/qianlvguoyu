package com.guoyu.platform.certification.vo;

import java.time.LocalDateTime;

public record CertificationArchiveImportErrorVO(
    Long id,
    Long batchId,
    String candidateNo,
    String name,
    String idCard,
    String maskedIdCard,
    String errorType,
    String errorMessage,
    LocalDateTime createdAt
) {
}
