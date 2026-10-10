package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;

public record CompetitionRankingEntryVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long rankingSnapshotId,
    @JsonSerialize(using = ToStringSerializer.class) Long contestantMemberId,
    @JsonSerialize(using = ToStringSerializer.class) Long totalScoreSnapshotId,
    BigDecimal totalScore,
    Integer displayRank,
    String tieKey,
    Integer tiedFlag,
    String contestantStatus,
    Integer sortOrder) {
}
