package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionRefereeGroupUpsertReq(
    @NotBlank @Size(max = 128) String groupName,
    @NotEmpty @Size(min = 1, max = 20) List<@NotNull Long> refereeProfileIds,
    @NotNull Long leaderRefereeProfileId) {
}
