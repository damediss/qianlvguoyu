package com.guoyu.platform.contract.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContractSourceResolveReq(
    @NotBlank @Size(max = 64) String knowledgeBaseId,
    @NotBlank @Size(max = 64) String knowledgeId,
    @AssertTrue boolean userConfirmed
) {
}
