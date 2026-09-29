package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionFormalCandidateAnswerSheetVO(
    String candidateExamId,
    String candidateName,
    String candidateNo,
    List<CompetitionFormalCandidateAnswerSheetQuestionVO> questions) {
}
