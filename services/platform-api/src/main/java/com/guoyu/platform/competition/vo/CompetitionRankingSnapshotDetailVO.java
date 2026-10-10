package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionRankingSnapshotDetailVO(
    CompetitionRankingSnapshotVO snapshot,
    List<CompetitionRankingEntryVO> entries) {
}
