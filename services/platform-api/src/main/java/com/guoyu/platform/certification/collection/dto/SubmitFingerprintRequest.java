package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmitFingerprintRequest(
    @NotNull Long taskId,
    @NotNull Long candidateId,
    Long sessionId,
    Long examSessionId,
    @Size(max = 128) String deviceSn,
    @Size(max = 128) String deviceModel,
    @Size(max = 32) String deviceVendorId,
    @Size(max = 32) String deviceProductId,
    @Size(max = 64) String sdkDeviceType,
    @Size(max = 32) String adapterType,
    @NotBlank @Size(max = 32) String collectStatus,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal qualityScore,
    @PositiveOrZero Long durationMs,
    @Size(max = 64) String errorCode,
    @Size(max = 500) String exceptionReason,
    @NotNull LocalDateTime collectedAt,
    Long fingerprintImageFileId,
    @Size(max = 128) String fingerprintImageHash,
    @Size(max = 16) String fingerprintImageFormat,
    LocalDateTime fingerprintImageCapturedAt,
    @Size(max = 64) String requestId
) {
}
