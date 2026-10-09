package com.guoyu.platform.competition.sorting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPlayerSortingRolesReq(
    @NotNull @Size(min = 1, max = 20) List<@NotBlank @Size(max = 16) String> roleOptions,
    @NotNull @Size(min = 1, max = 5000) List<@Valid Member> members) {

  public record Member(
      @NotNull @Positive Long contestantProfileId,
      @NotBlank @Size(max = 16) String role) {
  }
}
