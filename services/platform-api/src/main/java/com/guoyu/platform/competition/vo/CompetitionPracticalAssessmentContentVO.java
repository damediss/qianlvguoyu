package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionPracticalAssessmentContentVO(
    String id,
    Integer sortOrder,
    String contentHtml,
    BigDecimal totalScore,
    List<CompetitionPracticalAssessmentElementVO> elements
) {
}
