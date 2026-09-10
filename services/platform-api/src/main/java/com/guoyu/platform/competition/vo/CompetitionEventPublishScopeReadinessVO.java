package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

/** Readiness of one explicitly selected event work-type-level scope. */
public record CompetitionEventPublishScopeReadinessVO(
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    @JsonSerialize(using = ToStringSerializer.class) Long workTypeId,
    String workTypeName,
    @JsonSerialize(using = ToStringSerializer.class) Long levelId,
    String levelCode,
    String levelName,
    String stagePlanStatus,
    int stageCount,
    boolean ready,
    List<CompetitionEventPublishReadinessCheckVO> checks) {
}
