package com.guoyu.platform.competition.vo;

public record CompetitionTheoryCourseSourceVO(
    Long courseId,
    Long workTypeId,
    String title,
    Integer level,
    String version
) {
}
