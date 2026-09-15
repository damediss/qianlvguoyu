package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.guoyu.platform.competition.entity.CompetitionSimulationScoreImportBatchEntity;
import java.time.LocalDateTime;

public record CompetitionSimulationImportBatchVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    @JsonSerialize(using = ToStringSerializer.class) Long stageId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeLevelId,
    @JsonSerialize(using = ToStringSerializer.class) Long scoreRuleId,
    @JsonSerialize(using = ToStringSerializer.class) Long sourceFileId,
    String fileName,
    String batchStatus,
    Integer totalRows,
    Integer validRows,
    Integer errorRows,
    Integer confirmedRows,
    String correctionReason,
    String errorMessage,
    LocalDateTime confirmedAt,
    @JsonSerialize(using = ToStringSerializer.class) Long confirmedBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static CompetitionSimulationImportBatchVO from(
      CompetitionSimulationScoreImportBatchEntity entity) {
    return new CompetitionSimulationImportBatchVO(entity.getId(), entity.getEventId(),
        entity.getStageId(), entity.getEventWorkTypeId(), entity.getEventWorkTypeLevelId(),
        entity.getScoreRuleId(), entity.getSourceFileId(), entity.getFileName(),
        entity.getBatchStatus(), entity.getTotalRows(), entity.getValidRows(),
        entity.getErrorRows(), entity.getConfirmedRows(), entity.getCorrectionReason(),
        entity.getErrorMessage(), entity.getConfirmedAt(), entity.getConfirmedBy(),
        entity.getCreatedAt(), entity.getUpdatedAt());
  }
}
