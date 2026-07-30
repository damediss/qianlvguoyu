package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreparationCandidateDocumentSaveReq(
    @NotNull Long candidateId,
    @NotBlank String docCode,
    @NotBlank String docName,
    Boolean required,
    Long fileId,
    String fileName,
    String fileType,
    String remark
) {
}
