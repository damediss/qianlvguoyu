package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionQuestionVO(
    String id,
    String eventId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String contentType,
    String questionType,
    Integer sortOrder,
    String stem,
    String referenceAnswer,
    String fillAnswer,
    String optionsJson,
    Integer difficulty,
    String status,
    String sourceBatchId,
    String operationContent,
    String studentPrepare,
    String equipmentPrepare,
    List<CompetitionPracticalScoreItemVO> scoreItems
) {
}
