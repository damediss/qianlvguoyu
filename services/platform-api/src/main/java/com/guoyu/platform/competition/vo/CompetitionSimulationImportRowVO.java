package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.guoyu.platform.competition.entity.CompetitionSimulationScoreImportRowEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionSimulationImportRowVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long batchId,
    Integer rowNo,
    String candidateNo,
    @JsonSerialize(using = ToStringSerializer.class) Long contestantMemberId,
    BigDecimal simulationScore,
    String externalReference,
    LocalDateTime achievedAt,
    String remark,
    String validationStatus,
    String errorMessage,
    @JsonSerialize(using = ToStringSerializer.class) Long scoreSourceId) {

  public static CompetitionSimulationImportRowVO from(
      CompetitionSimulationScoreImportRowEntity entity) {
    return new CompetitionSimulationImportRowVO(entity.getId(), entity.getBatchId(),
        entity.getRowNo(), entity.getCandidateNo(), entity.getContestantMemberId(),
        entity.getSimulationScore(), entity.getExternalReference(), entity.getAchievedAt(),
        entity.getRemark(), entity.getValidationStatus(), entity.getErrorMessage(),
        entity.getScoreSourceId());
  }
}
