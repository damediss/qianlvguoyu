package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record SubmitSignatureRequest(
    @NotNull Long taskId,
    @NotNull Long candidateId,
    Long sessionId,
    @NotNull Long signatureFileId,
    @NotBlank @Size(min = 64, max = 64) String signatureHash,
    @NotBlank @Size(max = 32) String signerType,
    @NotBlank @Size(max = 64) String signerName,
    @NotNull LocalDateTime signedAt,
    @Size(max = 2000) String deviceInfo
) {
}
