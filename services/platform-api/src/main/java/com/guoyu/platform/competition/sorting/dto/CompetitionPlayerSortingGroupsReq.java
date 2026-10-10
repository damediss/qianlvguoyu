package com.guoyu.platform.competition.sorting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPlayerSortingGroupsReq(
    @NotNull @Size(min = 1, max = 200) List<@Valid Group> groups) {

  public record Group(
      @NotBlank @Size(max = 32) String groupName,
      @NotNull @Size(min = 1, max = 5000) List<@NotNull @Positive Long> contestantProfileIds) {
  }
}
