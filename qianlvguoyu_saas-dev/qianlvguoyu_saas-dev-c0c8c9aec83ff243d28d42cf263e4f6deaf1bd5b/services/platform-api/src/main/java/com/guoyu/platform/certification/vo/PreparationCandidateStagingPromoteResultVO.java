package com.guoyu.platform.certification.vo;

import java.util.List;

public record PreparationCandidateStagingPromoteResultVO(
    int total,
    int successCount,
    int failedCount,
    List<PreparationCandidateVO> candidates,
    List<PreparationStagingPromoteErrorVO> errors
) {
}
