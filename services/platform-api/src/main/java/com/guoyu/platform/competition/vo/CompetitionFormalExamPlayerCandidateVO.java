package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionFormalExamPlayerCandidateVO(
    String id,
    String status,
    String candidateId,
    String candidateNo,
    String candidateName,
    LocalDateTime submitTime) {
}
