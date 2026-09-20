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
    String scoringItemsJson,
    String practicalDetailJson,
    BigDecimal scoreDifference,
    String scoringMethod,
    String blueprintId,
    String blueprintNodeId,
    String blueprintFullCode,
    String blueprintPath,
    BigDecimal sourceTotalScore,
    BigDecimal scoreScale
) {
  public CompetitionMakeExamSnapshotQuestionVO(
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
      String scoringItemsJson) {
    this(id, sourceQuestionId, sortOrder, questionType, stem, optionsJson,
        referenceAnswer, fillAnswer, score, difficulty, operationContent,
        studentPrepare, equipmentPrepare, scoringItemsJson, null, null, null, null, null, null, null, null, null);
  }
}
