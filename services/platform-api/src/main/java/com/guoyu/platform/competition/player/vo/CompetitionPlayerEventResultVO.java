package com.guoyu.platform.competition.player.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionPlayerEventResultVO(
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    String eventName,
    String status,
    LocalDateTime latestPublishedAt,
    List<ResultItem> results) {

  public record ResultItem(
      @JsonSerialize(using = ToStringSerializer.class) Long entryId,
      @JsonSerialize(using = ToStringSerializer.class) Long snapshotId,
      Integer snapshotVersion,
      String stageName,
      String workTypeName,
      String levelName,
      BigDecimal totalScore,
      Integer displayRank,
      Integer tiedFlag,
      String contestantStatus,
      LocalDateTime publishedAt,
      List<SubjectScore> subjects) {
  }

  public record SubjectScore(String code, String name, BigDecimal score) {
  }
}
