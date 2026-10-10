package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerContextVO(
    CompetitionFormalExamPlayerSessionVO examSession,
    CompetitionFormalExamPlayerCandidateVO candidateExam,
    CompetitionFormalExamPlayerCompetitionVO competition,
    LocalDateTime serverTime,
    String runtimeIssue) {
}
