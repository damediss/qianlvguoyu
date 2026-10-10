package com.guoyu.platform.competition.vo;

public record CompetitionScoringReadinessCheckVO(
    String code,
    boolean passed,
    String message
) {
}
