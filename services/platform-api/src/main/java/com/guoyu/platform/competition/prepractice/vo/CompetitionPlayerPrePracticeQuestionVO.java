package com.guoyu.platform.competition.prepractice.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record CompetitionPlayerPrePracticeQuestionVO(
    String questionId,
    Integer sortOrder,
    String questionType,
    String questionTypeName,
    boolean objective,
    String content,
    String audio,
    String video,
    List<CompetitionPlayerPrePracticeOptionVO> options,
    JsonNode userAnswer,
    boolean answered,
    String judgmentStatus,
    Boolean correct) {
}
