package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;

/** Complete, database-derived preflight result for the event publish command. */
public record CompetitionEventPublishReadinessVO(
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    String eventCode,
    String eventName,
    String eventStatus,
    String eventScope,
    LocalDateTime startAt,
    LocalDateTime endAt,
    boolean ready,
    int activeWorkTypeCount,
    int selectedLevelCount,
    int confirmedScopeCount,
    int stageCount,
    List<CompetitionEventPublishReadinessCheckVO> eventChecks,
    List<CompetitionEventPublishScopeReadinessVO> scopes) {
}
