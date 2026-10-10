package com.guoyu.platform.competition.messaging.model;

public record CompetitionScorePipelineRequestedEvent(Long tenantId, Long scoreSourceId, int attemptNo) {
}
