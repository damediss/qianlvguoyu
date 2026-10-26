package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.guoyu.platform.competition.entity.CompetitionSimulationProviderBindingEntity;
import java.time.LocalDateTime;

public record CompetitionSimulationProviderBindingVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    @JsonSerialize(using = ToStringSerializer.class) Long stageId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeLevelId,
    String providerCode,
    String externalHostTenantId,
    String externalSessionId,
    String moduleFilter,
    String status,
    LocalDateTime lastSyncAt) {

  public static CompetitionSimulationProviderBindingVO from(
      CompetitionSimulationProviderBindingEntity entity) {
    if (entity == null) return null;
    return new CompetitionSimulationProviderBindingVO(
        entity.getId(), entity.getEventId(), entity.getStageId(), entity.getEventWorkTypeId(),
        entity.getEventWorkTypeLevelId(), entity.getProviderCode(),
        entity.getExternalHostTenantId(), entity.getExternalSessionId(), entity.getModuleFilter(),
        entity.getStatus(), entity.getLastSyncAt());
  }
}
