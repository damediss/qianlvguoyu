package com.guoyu.platform.certification.dto;

import java.util.List;

public record PreparationAssessorTaskDraftReq(
    Long assessorId,
    String progressNote,
    List<PreparationScoreReq> scores
) {
}
