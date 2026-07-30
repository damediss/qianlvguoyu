package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.enums.CompetitionRankingContestantStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CompetitionRankingAllocator {

  public List<RankedCandidate> allocate(
      RankingScope snapshotScope, List<RankingCandidate> sortedCandidates) {
    validateScope(snapshotScope);
    List<RankingCandidate> candidates =
        sortedCandidates == null ? Collections.emptyList() : sortedCandidates;
    List<RankedCandidate> ranked = new ArrayList<>();
    int rankedPosition = 0;
    Integer currentRank = null;
    String currentTieKey = null;
    List<Integer> currentTieGroupIndexes = new ArrayList<>();

    for (RankingCandidate candidate : candidates) {
      validateCandidate(snapshotScope, candidate);
      if (candidate.status() == CompetitionRankingContestantStatus.ABSENT) {
        ranked.add(new RankedCandidate(
            candidate.contestantMemberId(),
            BigDecimal.ZERO,
            null,
            false,
            candidate.status(),
            candidate.tieKey()));
        continue;
      }

      rankedPosition++;
      boolean tiedWithPrevious = currentTieKey != null
          && currentTieKey.equals(candidate.tieKey());
      if (!tiedWithPrevious) {
        currentRank = rankedPosition;
        currentTieKey = candidate.tieKey();
        currentTieGroupIndexes.clear();
      } else {
        for (Integer index : currentTieGroupIndexes) {
          RankedCandidate previous = ranked.get(index);
          ranked.set(index, previous.withTied(true));
        }
      }
      RankedCandidate result = new RankedCandidate(
          candidate.contestantMemberId(),
          candidate.totalScore(),
          currentRank,
          tiedWithPrevious,
          candidate.status(),
          candidate.tieKey());
      ranked.add(result);
      currentTieGroupIndexes.add(ranked.size() - 1);
    }
    return List.copyOf(ranked);
  }

  private void validateScope(RankingScope scope) {
    if (scope == null
        || scope.eventId() == null
        || scope.stageId() == null
        || scope.eventWorkTypeId() == null
        || scope.scoreRuleId() == null) {
      throw new IllegalArgumentException("排名快照范围必须完整");
    }
  }

  private void validateCandidate(RankingScope snapshotScope, RankingCandidate candidate) {
    if (candidate == null
        || candidate.contestantMemberId() == null
        || candidate.status() == null
        || !Objects.equals(snapshotScope, candidate.scope())) {
      throw new IllegalArgumentException("选手不属于当前赛事、赛段、工种和规则版本范围");
    }
    if (candidate.status() == CompetitionRankingContestantStatus.COMPLETED) {
      if (candidate.tieKey() == null || candidate.tieKey().isBlank()) {
        throw new IllegalArgumentException("正常完赛记录必须提供已冻结排序键摘要");
      }
      if (candidate.totalScore() == null
          || candidate.totalScore().compareTo(BigDecimal.ZERO) < 0
          || candidate.totalScore().compareTo(new BigDecimal("100")) > 0) {
        throw new IllegalArgumentException("正常完赛总分必须在0到100之间");
      }
    }
  }

  public record RankingScope(
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long scoreRuleId) {
  }

  public record RankingCandidate(
      RankingScope scope,
      Long contestantMemberId,
      BigDecimal totalScore,
      String tieKey,
      CompetitionRankingContestantStatus status) {
  }

  public record RankedCandidate(
      Long contestantMemberId,
      BigDecimal totalScore,
      Integer rank,
      boolean tied,
      CompetitionRankingContestantStatus status,
      String tieKey) {

    private RankedCandidate withTied(boolean tied) {
      return new RankedCandidate(
          contestantMemberId, totalScore, rank, tied, status, tieKey);
    }
  }
}
