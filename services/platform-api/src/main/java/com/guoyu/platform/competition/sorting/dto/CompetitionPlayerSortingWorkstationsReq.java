package com.guoyu.platform.competition.sorting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPlayerSortingWorkstationsReq(
    @NotBlank @Size(max = 16) String label,
    @NotBlank @Size(max = 16) String subLabel,
    @NotNull @Size(min = 1, max = 500) List<@Valid Station> stations) {

  public record Station(
      @NotBlank @Size(max = 32) String code,
      @NotNull @Size(min = 1, max = 50) List<@NotBlank @Size(max = 32) String> subCodes) {
  }
}
