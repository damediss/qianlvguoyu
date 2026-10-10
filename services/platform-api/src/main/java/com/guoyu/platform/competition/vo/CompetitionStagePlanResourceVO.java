package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionStagePlanResourceVO(
    Long id,
    Long planId,
    String contentType,
    String resourceType,
    Long resourceId,
    String resourceCode,
    String resourceVersion,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String resourcePayload,
    String status) {
}
