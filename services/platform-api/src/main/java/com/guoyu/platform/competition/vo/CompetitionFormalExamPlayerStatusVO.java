package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerStatusVO(
    CompetitionFormalExamPlayerSessionVO examSession,
    CompetitionFormalExamPlayerStatusCandidateVO candidateExam,
    LocalDateTime serverTime,
    String runtimeIssue) {
}
