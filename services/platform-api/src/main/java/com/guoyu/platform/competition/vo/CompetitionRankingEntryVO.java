package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionRankingEntryVO(
    Long id,
    Long rankingSnapshotId,
    Long contestantMemberId,
    Long totalScoreSnapshotId,
    BigDecimal totalScore,
    Integer displayRank,
    String tieKey,
    Integer tiedFlag,
    String contestantStatus,
    Integer sortOrder) {
}
