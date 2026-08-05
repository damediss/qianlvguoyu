package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.entity.CompetitionRankingEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import com.guoyu.platform.competition.vo.CompetitionRankingEntryVO;
import com.guoyu.platform.competition.vo.CompetitionRankingSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionScoreSourceVO;
import com.guoyu.platform.competition.vo.CompetitionTotalScoreSnapshotVO;

public final class CompetitionScoringConverter {

  private CompetitionScoringConverter() {
  }

  public static CompetitionScoreSourceVO toVO(CompetitionScoreSourceEntity entity) {
    return new CompetitionScoreSourceVO(
        entity.getId(), entity.getEventId(), entity.getStageId(),
        entity.getEventWorkTypeId(), entity.getEventWorkTypeLevelId(), entity.getContestantMemberId(),
        entity.getSourceType(), entity.getRawScore(), entity.getConfirmationStatus(),
        entity.getSourceSystem(), entity.getSourceReference(), entity.getScoreRuleId(),
        entity.getRevisionNo(), entity.getCurrentFlag(), entity.getAchievedAt(),
        entity.getConfirmedAt(), entity.getConfirmedBy(), entity.getCreatedAt());
  }

  public static CompetitionTotalScoreSnapshotVO toVO(
      CompetitionTotalScoreSnapshotEntity entity) {
    return new CompetitionTotalScoreSnapshotVO(
        entity.getId(), entity.getEventId(), entity.getStageId(),
        entity.getEventWorkTypeId(), entity.getEventWorkTypeLevelId(), entity.getContestantMemberId(), entity.getScoreRuleId(),
        entity.getTheorySourceId(), entity.getSimulationSourceId(),
        entity.getPracticalSourceId(), entity.getTheoryScore(),
        entity.getSimulationScore(), entity.getPracticalScore(),
        entity.getSimulationPracticalScore(), entity.getTheoryWeight(),
        entity.getSimulationWeight(), entity.getPracticalWeight(), entity.getSimulationPracticalWeight(), entity.getTotalScore(),
        entity.getCalculationVersion(), entity.getConfirmationStatus(),
        entity.getCurrentFlag(), entity.getCalculatedAt(), entity.getCreatedAt());
  }

  public static CompetitionRankingSnapshotVO toVO(CompetitionRankingSnapshotEntity entity) {
    return new CompetitionRankingSnapshotVO(
        entity.getId(), entity.getEventId(), entity.getStageId(),
        entity.getEventWorkTypeId(), entity.getEventWorkTypeLevelId(), entity.getScoreRuleId(),
        entity.getSnapshotVersion(), entity.getStatus(), entity.getGenerationReason(),
        entity.getGeneratedAt(), entity.getCreatedAt());
  }

  public static CompetitionRankingEntryVO toVO(CompetitionRankingEntryEntity entity) {
    return new CompetitionRankingEntryVO(
        entity.getId(), entity.getRankingSnapshotId(), entity.getContestantMemberId(),
        entity.getTotalScoreSnapshotId(), entity.getTotalScore(), entity.getDisplayRank(),
        entity.getTieKey(), entity.getTiedFlag(), entity.getContestantStatus(),
        entity.getSortOrder());
  }
}
