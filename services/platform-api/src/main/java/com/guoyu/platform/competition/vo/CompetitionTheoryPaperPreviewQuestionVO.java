package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

/**
 * The deliberately restricted question projection used by the paper preview.
 * It must not expose content, options, answers, or explanations.
 */
public record CompetitionTheoryPaperPreviewQuestionVO(
    Integer sortOrder,
    String content,
    String questionType,
    BigDecimal score
) {
}
