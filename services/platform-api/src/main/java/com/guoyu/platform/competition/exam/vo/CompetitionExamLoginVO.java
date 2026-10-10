package com.guoyu.platform.competition.exam.vo;

public record CompetitionExamLoginVO(
    Long playerId,
    Long tenantId,
    String accessToken,
    String tokenType,
    long expiresIn,
    Long attemptId) {
}
