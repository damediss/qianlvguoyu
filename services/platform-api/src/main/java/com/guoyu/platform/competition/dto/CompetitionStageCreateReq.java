package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record CompetitionStageCreateReq(
    /** Deprecated input retained for source compatibility; the server derives the name. */
    @Pattern(regexp = ".*\\S.*", message = "赛段名称不能是空白") @Size(max = 128) String name,
    @jakarta.validation.constraints.NotBlank @Size(max = 32) String stageType,
    /** Deprecated input retained for source compatibility; the server derives the order. */
    @Min(0) Integer sortOrder,
    LocalDateTime startAt,
    LocalDateTime endAt,
    /** Deprecated input retained for source compatibility; clients cannot set lifecycle state. */
    String status,
    @Size(max = 2000) String description
) {

  public CompetitionStageCreateReq(
      String stageType,
      LocalDateTime startAt,
      LocalDateTime endAt,
      String description) {
    this(null, stageType, null, startAt, endAt, null, description);
  }
}
