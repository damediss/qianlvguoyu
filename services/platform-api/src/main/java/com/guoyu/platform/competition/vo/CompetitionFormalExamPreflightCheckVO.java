package com.guoyu.platform.competition.vo;

public record CompetitionFormalExamPreflightCheckVO(
    String code,
    boolean passed,
    String message) {
}
