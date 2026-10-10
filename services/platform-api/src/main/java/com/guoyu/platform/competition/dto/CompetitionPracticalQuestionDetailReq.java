package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/** Practical question metadata and the structured scoring table. */
public record CompetitionPracticalQuestionDetailReq(
    Integer durationMinutes,
    BigDecimal scoreDifference,
    BigDecimal totalScore,
    String scoringMethod,
    String assessmentRequirementsHtml,
    String examRoomPrepareHtml,
    String candidatePrepareHtml,
    String negativeItemsHtml,
    String questionMaterialsHtml,
    @Valid List<@Valid CompetitionPracticalAssessmentContentReq> assessmentContents,
    // Legacy fields are accepted for one migration cycle and converted to the new names.
    String operationContent,
    String studentPrepare,
    String equipmentPrepare
) {

  /** Source-compatible constructor used by older import/test callers. */
  public CompetitionPracticalQuestionDetailReq(
      String operationContent, String studentPrepare, String equipmentPrepare) {
    this(null, null, null, null, null, null, null, null, null, null,
        operationContent, studentPrepare, equipmentPrepare);
  }
}
