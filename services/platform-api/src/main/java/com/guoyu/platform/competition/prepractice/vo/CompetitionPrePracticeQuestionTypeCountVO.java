package com.guoyu.platform.competition.prepractice.vo;

public record CompetitionPrePracticeQuestionTypeCountVO(
    String questionType,
    String questionTypeName,
    Integer questionCount,
    Long availableCount) {
}
