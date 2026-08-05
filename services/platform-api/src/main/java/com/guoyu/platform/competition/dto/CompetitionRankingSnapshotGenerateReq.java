package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionRankingSnapshotGenerateReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    @NotNull Long scoreRuleId,
    @Size(max = 500) String generationReason,
    @NotNull @Size(min = 1, max = 500) List<@Valid Candidate> candidates) {

  public CompetitionRankingSnapshotGenerateReq(
      Long stageId,
      Long eventWorkTypeId,
      Long scoreRuleId,
      String generationReason,
      List<@Valid Candidate> candidates) {
    this(stageId, eventWorkTypeId, null, scoreRuleId, generationReason, candidates);
  }

  public record Candidate(
      @NotNull Long contestantMemberId,
      Long totalScoreSnapshotId,
      @Size(max = 255) String tieKey,
      @NotNull @Pattern(regexp = "completed|absent") String contestantStatus) {
  }
}
