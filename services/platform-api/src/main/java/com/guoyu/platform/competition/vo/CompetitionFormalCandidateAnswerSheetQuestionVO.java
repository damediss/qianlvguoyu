package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record CompetitionFormalCandidateAnswerSheetQuestionVO(
    String questionSnapshotId,
    Integer questionOrder,
    String questionType,
    String stem,
    Boolean answered,
    Boolean correct,
    JsonNode candidateAnswer,
    List<CompetitionFormalExamPlayerOptionVO> options) {
}
