package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionPracticalQuestionDetailVO(
    Integer durationMinutes,
    BigDecimal scoreDifference,
    BigDecimal totalScore,
    String scoringMethod,
    String assessmentRequirementsHtml,
    String examRoomPrepareHtml,
    String candidatePrepareHtml,
    String negativeItemsHtml,
    String questionMaterialsHtml,
    List<CompetitionPracticalAssessmentContentVO> assessmentContents
) {
}
