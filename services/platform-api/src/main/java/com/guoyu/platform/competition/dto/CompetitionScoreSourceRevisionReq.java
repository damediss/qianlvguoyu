package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionScoreSourceRevisionReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    @NotNull Long contestantMemberId,
    @NotBlank @Pattern(regexp = "theory|simulation|practical") String sourceType,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal rawScore,
    @NotBlank @Pattern(regexp = "pending|confirmed|voided") String confirmationStatus,
    @Size(max = 64) String sourceSystem,
    @Size(max = 255) String sourceReference,
    @NotNull Long scoreRuleId,
    @NotNull LocalDateTime achievedAt) {

  public CompetitionScoreSourceRevisionReq(
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId,
      String sourceType,
      BigDecimal rawScore,
      String confirmationStatus,
      String sourceSystem,
      String sourceReference,
      Long scoreRuleId,
      LocalDateTime achievedAt) {
    this(stageId, eventWorkTypeId, null, contestantMemberId, sourceType, rawScore,
        confirmationStatus, sourceSystem, sourceReference, scoreRuleId, achievedAt);
  }
}
