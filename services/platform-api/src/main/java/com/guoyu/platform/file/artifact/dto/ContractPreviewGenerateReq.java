package com.guoyu.platform.file.artifact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContractPreviewGenerateReq(
    @NotBlank @Size(max = 120) String partyA,
    @NotBlank @Size(max = 120) String partyB,
    @NotBlank @Size(max = 80) String amount,
    @Size(max = 120) String contractTitle,
    @Size(max = 2000) String serviceScope
) {
}
