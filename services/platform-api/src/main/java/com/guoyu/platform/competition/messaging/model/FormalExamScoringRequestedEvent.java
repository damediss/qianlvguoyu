package com.guoyu.platform.competition.messaging.model;

public record FormalExamScoringRequestedEvent(
    Long tenantId, Long taskId, Long examSessionId, Long candidateExamId, Integer revisionNo) {
}
