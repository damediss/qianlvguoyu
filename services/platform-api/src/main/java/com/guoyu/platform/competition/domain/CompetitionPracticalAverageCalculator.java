package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.enums.CompetitionPracticalScoreStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CompetitionPracticalAverageCalculator {

  private static final int REQUIRED_REFEREE_COUNT = 3;

  public PracticalAverageResult calculate(List<RefereeScore> scores) {
    return calculate(scores, null);
  }

  /**
   * Calculates the three-referee average and optionally enforces the frozen
   * max-minus-min threshold from the practical paper snapshot.
   */
  public PracticalAverageResult calculate(List<RefereeScore> scores, BigDecimal scoreDifference) {
    List<RefereeScore> candidates = scores == null ? Collections.emptyList() : scores;
    List<RefereeScore> effective = candidates.stream()
        .filter(RefereeScore::currentRevision)
        .filter(score -> score.status() == CompetitionPracticalScoreStatus.SUBMITTED_LOCKED)
        .toList();

    Set<Long> refereeIds = new HashSet<>();
    for (RefereeScore score : effective) {
      if (score.refereeMemberId() == null || !refereeIds.add(score.refereeMemberId())) {
        return PracticalAverageResult.pending(effective.size());
      }
      validateScore(score.score());
    }
    if (effective.size() != REQUIRED_REFEREE_COUNT) {
      return PracticalAverageResult.pending(effective.size());
    }
    BigDecimal total = effective.stream()
        .map(RefereeScore::score)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (scoreDifference != null && scoreDifference.signum() >= 0) {
      BigDecimal max = effective.stream().map(RefereeScore::score).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
      BigDecimal min = effective.stream().map(RefereeScore::score).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
      if (max.subtract(min).compareTo(scoreDifference) > 0) {
        return PracticalAverageResult.pendingScoreDifference(effective.size());
      }
    }
    return PracticalAverageResult.confirmed(
        total.divide(BigDecimal.valueOf(REQUIRED_REFEREE_COUNT), 2, RoundingMode.HALF_UP));
  }

  private void validateScore(BigDecimal score) {
    if (score == null
        || score.compareTo(BigDecimal.ZERO) < 0
        || score.compareTo(new BigDecimal("100")) > 0) {
      throw new IllegalArgumentException("有效裁判分必须在0到100之间");
    }
  }

  public record RefereeScore(
      Long refereeMemberId,
      BigDecimal score,
      CompetitionPracticalScoreStatus status,
      boolean currentRevision) {
  }

  public record PracticalAverageResult(
      PracticalAverageStatus status,
      BigDecimal score,
      int effectiveRefereeCount) {

    private static PracticalAverageResult confirmed(BigDecimal score) {
      return new PracticalAverageResult(PracticalAverageStatus.CONFIRMED, score, 3);
    }

    private static PracticalAverageResult pending(int effectiveRefereeCount) {
      return new PracticalAverageResult(
          PracticalAverageStatus.PENDING_REFEREES_INCOMPLETE, null, effectiveRefereeCount);
    }

    private static PracticalAverageResult pendingScoreDifference(int effectiveRefereeCount) {
      return new PracticalAverageResult(
          PracticalAverageStatus.PENDING_SCORE_DIFFERENCE, null, effectiveRefereeCount);
    }

    public boolean confirmed() {
      return status == PracticalAverageStatus.CONFIRMED;
    }
  }

  public enum PracticalAverageStatus {
    CONFIRMED,
    PENDING_REFEREES_INCOMPLETE,
    PENDING_SCORE_DIFFERENCE
  }
}
