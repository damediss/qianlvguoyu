package com.guoyu.platform.competition.dto;

import java.util.List;

public record CompetitionFormalExamPlayerSubmitReq(
    Boolean confirmUnanswered,
    List<CompetitionFormalExamPlayerAnswerVersionItem> answerVersions) {
}
