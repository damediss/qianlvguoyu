package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.guoyu.platform.competition.entity.CompetitionSimulationSyncBatchEntity;
import java.time.LocalDateTime;

public record CompetitionSimulationSyncBatchVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long bindingId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    @JsonSerialize(using = ToStringSerializer.class) Long stageId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeLevelId,
    String batchStatus,
    Integer totalRows,
    Integer validRows,
    Integer errorRows,
    Integer confirmedRows,
    String correctionReason,
    String errorMessage,
    LocalDateTime confirmedAt,
    @JsonSerialize(using = ToStringSerializer.class) Long confirmedBy,
    LocalDateTime createdAt) {

  public static CompetitionSimulationSyncBatchVO from(
      CompetitionSimulationSyncBatchEntity entity) {
    return new CompetitionSimulationSyncBatchVO(
        entity.getId(), entity.getBindingId(), entity.getEventId(), entity.getStageId(),
        entity.getEventWorkTypeId(), entity.getEventWorkTypeLevelId(), entity.getBatchStatus(),
        entity.getTotalRows(), entity.getValidRows(), entity.getErrorRows(),
        entity.getConfirmedRows(), entity.getCorrectionReason(), entity.getErrorMessage(),
        entity.getConfirmedAt(), entity.getConfirmedBy(), entity.getCreatedAt());
  }
}
