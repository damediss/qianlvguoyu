package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.guoyu.platform.competition.entity.CompetitionSubjectScoreImportBatchEntity;
import java.time.LocalDateTime;

public record CompetitionSubjectScoreImportBatchVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    @JsonSerialize(using = ToStringSerializer.class) Long stageId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeLevelId,
    String sourceType,
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
    @JsonSerialize(using = ToStringSerializer.class) Long confirmedBy) {

  public static CompetitionSubjectScoreImportBatchVO from(
      CompetitionSubjectScoreImportBatchEntity entity) {
    return new CompetitionSubjectScoreImportBatchVO(
        entity.getId(), entity.getEventId(), entity.getStageId(),
        entity.getEventWorkTypeId(), entity.getEventWorkTypeLevelId(),
        entity.getSourceType(), entity.getSourceFileId(), entity.getFileName(),
        entity.getBatchStatus(), entity.getTotalRows(), entity.getValidRows(),
        entity.getErrorRows(), entity.getConfirmedRows(), entity.getCorrectionReason(),
        entity.getErrorMessage(), entity.getConfirmedAt(), entity.getConfirmedBy());
  }
}
