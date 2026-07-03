package com.guoyu.platform.certification.collection.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FingerprintTraceVO(
    Long id,
    Long taskId,
    Long candidateId,
    Long sessionId,
    String deviceSn,
    String deviceModel,
    String deviceVendorId,
    String deviceProductId,
    String sdkDeviceType,
    String adapterType,
    String collectStatus,
    String collectStatusLabel,
    BigDecimal qualityScore,
    Long durationMs,
    String errorCode,
    String exceptionReason,
    LocalDateTime collectedAt,
    Long fingerprintImageFileId,
    String fingerprintImageHash,
    String fingerprintImageFormat,
    LocalDateTime fingerprintImageCapturedAt
) {
}
