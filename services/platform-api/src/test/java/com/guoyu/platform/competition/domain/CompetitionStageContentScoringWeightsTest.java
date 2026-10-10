package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionStageContentScoringWeightsTest {

  @Test
  void practicalOnlyAtHundredIsACompleteFormula() {
    var weights = CompetitionStageContentScoringWeights.from(List.of(
        content("practical", "enabled", "100")));

    assertThat(weights.valid()).isTrue();
    assertThat(weights.theoryOnly()).isFalse();
    assertThat(weights.enabled("practical")).isTrue();
    assertThat(weights.enabled("theory")).isFalse();
    assertThat(weights.practicalWeight()).isEqualByComparingTo("100");
    assertThat(weights.theoryWeight()).isEqualByComparingTo("0");
    assertThat(weights.simulationWeight()).isEqualByComparingTo("0");
  }

  @Test
  void mixedEnabledWeightsMustSumToOneHundred() {
    var weights = CompetitionStageContentScoringWeights.from(List.of(
        content("theory", "enabled", "40"),
        content("practical", "enabled", "60"),
        content("simulation", "disabled", "0")));

    assertThat(weights.valid()).isTrue();
    assertThat(weights.theoryWeight()).isEqualByComparingTo("40");
    assertThat(weights.practicalWeight()).isEqualByComparingTo("60");
    assertThat(weights.combinedSimulationPracticalWeight()).isEqualByComparingTo("60");
  }

  @Test
  void rejectsEmptyOrNonHundredWeights() {
    assertThat(CompetitionStageContentScoringWeights.from(List.of()).valid()).isFalse();
    assertThat(CompetitionStageContentScoringWeights.from(List.of(
        content("practical", "enabled", "80"))).valid()).isFalse();
    assertThat(CompetitionStageContentScoringWeights.from(List.of(
        content("practical", "disabled", "100"))).valid()).isFalse();
  }

  private CompetitionStageContentEntity content(String type, String status, String weight) {
    CompetitionStageContentEntity entity = new CompetitionStageContentEntity();
    entity.setContentType(type);
    entity.setStatus(status);
    entity.setWeight(new BigDecimal(weight));
    return entity;
  }
}
