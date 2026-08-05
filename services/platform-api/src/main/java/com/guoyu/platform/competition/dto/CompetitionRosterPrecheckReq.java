package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionRosterPrecheckReq(
    @NotNull Long fileId,
    @NotNull Long orgId,
    @NotNull Long eventWorkTypeId
) {
  public CompetitionRosterPrecheckReq(Long fileId, Long orgId) {
    this(fileId, orgId, null);
  }
}
