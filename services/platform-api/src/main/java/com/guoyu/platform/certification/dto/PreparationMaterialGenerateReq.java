package com.guoyu.platform.certification.dto;

import java.util.List;

public record PreparationMaterialGenerateReq(
    String generateScope,
    List<Long> candidateIds,
    String remark
) {
}
