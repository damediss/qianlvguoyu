package com.guoyu.platform.competition.vo;

public record CompetitionStageReadinessCheckVO(
    String code,
    boolean passed,
    String message) {
}
