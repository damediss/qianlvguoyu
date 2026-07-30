package com.guoyu.platform.certification.collection.vo;

import java.time.LocalDateTime;

public record CollectionExceptionVO(
    Long id,
    Long taskId,
    Long candidateId,
    String candidateName,
    String idCardLast4,
    String exceptionType,
    String exceptionTypeLabel,
    String exceptionStatus,
    String exceptionStatusLabel,
    String releaseScope,
    String releaseScopeLabel,
    String reason,
    String deviceSn,
    String deviceStatus,
    Long handledBy,
    LocalDateTime handledAt,
    LocalDateTime createdAt
) {
}
