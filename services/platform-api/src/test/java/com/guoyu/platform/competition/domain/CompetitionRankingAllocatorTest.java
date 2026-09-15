package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.competition.domain.CompetitionRankingAllocator.RankingCandidate;
import com.guoyu.platform.competition.domain.CompetitionRankingAllocator.RankingScope;
import com.guoyu.platform.competition.enums.CompetitionRankingContestantStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionRankingAllocatorTest {

  private final CompetitionRankingAllocator allocator = new CompetitionRankingAllocator();
  private final RankingScope scope = new RankingScope(10L, 20L, 30L, 40L);

  @Test
  void allocatesOneOneThreeFour() {
    var result = allocator.allocate(scope, List.of(
        completed(1L, "100", "100"),
        completed(2L, "100", "100"),
        completed(3L, "90", "90"),
        completed(4L, "80", "80")));

    assertThat(result).extracting(item -> item.rank()).containsExactly(2, 2, 3, 4);
    assertThat(result).extracting(item -> item.tied()).containsExactly(true, true, false, false);
  }

  @Test
  void allocatesOneThreeThreeFour() {
    var result = allocator.allocate(scope, List.of(
        completed(1L, "100", "100"),
        completed(2L, "90", "90"),
        completed(3L, "90", "90"),
        completed(4L, "80", "80")));

    assertThat(result).extracting(item -> item.rank()).containsExactly(1, 3, 3, 4);
  }

  @Test
  void keepsAbsentContestantWithoutConsumingRank() {
    var result = allocator.allocate(scope, List.of(
        completed(1L, "100", "100"),
        absent(9L),
        completed(2L, "90", "90")));

    assertThat(result).extracting(item -> item.rank()).containsExactly(1, null, 2);
    assertThat(result.get(1).totalScore()).isEqualByComparingTo("0");
    assertThat(result.get(1).status()).isEqualTo(CompetitionRankingContestantStatus.ABSENT);
  }

  @Test
  void rejectsCandidateFromDifferentWorkTypeScope() {
    RankingScope otherWorkType = new RankingScope(10L, 20L, 31L, 40L);
    RankingCandidate candidate = new RankingCandidate(
        otherWorkType, 1L, new BigDecimal("100"), "100",
        CompetitionRankingContestantStatus.COMPLETED);

    assertThatThrownBy(() -> allocator.allocate(scope, List.of(candidate)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("不属于当前");
  }

  private RankingCandidate completed(Long contestantId, String score, String tieKey) {
    return new RankingCandidate(
        scope,
        contestantId,
        new BigDecimal(score),
        tieKey,
        CompetitionRankingContestantStatus.COMPLETED);
  }

  private RankingCandidate absent(Long contestantId) {
    return new RankingCandidate(
        scope,
        contestantId,
        BigDecimal.ZERO,
        null,
        CompetitionRankingContestantStatus.ABSENT);
  }
}
