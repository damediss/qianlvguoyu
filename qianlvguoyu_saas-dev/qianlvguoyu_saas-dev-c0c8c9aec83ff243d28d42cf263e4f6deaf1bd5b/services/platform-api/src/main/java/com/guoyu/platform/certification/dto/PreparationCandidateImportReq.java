package com.guoyu.platform.certification.dto;

import java.util.List;

public record PreparationCandidateImportReq(
    String sourceType,
    Long sourceFileId,
    List<PreparationCandidateSaveReq> candidates,
    String remark
) {
}
