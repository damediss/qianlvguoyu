package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionMakeExamSnapshotQuestionVO(
    String id,
    String sourceQuestionId,
    Integer sortOrder,
    String questionType,
    String stem,
    String optionsJson,
    String referenceAnswer,
    String fillAnswer,
    BigDecimal score,
    Integer difficulty,
    String operationContent,
    String studentPrepare,
    String equipmentPrepare,
    String scoringItemsJson
) {
}
