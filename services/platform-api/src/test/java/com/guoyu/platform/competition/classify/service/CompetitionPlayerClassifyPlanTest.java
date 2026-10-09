package com.guoyu.platform.competition.classify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionPlayerClassifyPlanTest {

  @Test
  void evenSplitGivesEachTypeTheSameCount() {
    assertEquals(List.of(16, 16), CompetitionPlayerClassifyPlan.balancedSizes(32, 2));
  }

  @Test
  void remainderGoesToEarlierTypesAndDiffersByAtMostOne() {
    assertEquals(List.of(11, 11, 10), CompetitionPlayerClassifyPlan.balancedSizes(32, 3));
    assertEquals(List.of(2, 2, 1), CompetitionPlayerClassifyPlan.balancedSizes(5, 3));
  }

  @Test
  void rejectsMoreTypesThanPlayers() {
    assertThrows(IllegalArgumentException.class,
        () -> CompetitionPlayerClassifyPlan.balancedSizes(2, 3));
  }
}
