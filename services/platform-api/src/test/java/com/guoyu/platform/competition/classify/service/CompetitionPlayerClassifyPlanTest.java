package com.guoyu.platform.competition.classify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
  void shuffledOrderKeepsTheBalancedCounts() {
    List<Integer> indexes = CompetitionPlayerClassifyPlan.shuffledTypeIndexes(32, 2, new Random(1));
    assertEquals(32, indexes.size());
    assertEquals(16, Collections.frequency(indexes, 0));
    assertEquals(16, Collections.frequency(indexes, 1));
    assertNotEquals(grouped(16, 16), indexes);
    assertEquals(shuffledBag(List.of(16, 16), 1), indexes);
  }

  @Test
  void unevenShuffleStillGivesEarlierTypesTheExtraSeat() {
    List<Integer> indexes = CompetitionPlayerClassifyPlan.shuffledTypeIndexes(5, 3, new Random(3));
    assertEquals(2, Collections.frequency(indexes, 0));
    assertEquals(2, Collections.frequency(indexes, 1));
    assertEquals(1, Collections.frequency(indexes, 2));
    assertEquals(shuffledBag(List.of(2, 2, 1), 3), indexes);
  }

  @Test
  void rejectsMoreTypesThanPlayers() {
    assertThrows(IllegalArgumentException.class,
        () -> CompetitionPlayerClassifyPlan.balancedSizes(2, 3));
    assertThrows(IllegalArgumentException.class,
        () -> CompetitionPlayerClassifyPlan.shuffledTypeIndexes(2, 3, new Random(1)));
  }

  private List<Integer> grouped(int first, int second) {
    List<Integer> indexes = new ArrayList<>();
    indexes.addAll(Collections.nCopies(first, 0));
    indexes.addAll(Collections.nCopies(second, 1));
    return indexes;
  }

  private List<Integer> shuffledBag(List<Integer> sizes, long seed) {
    List<Integer> indexes = new ArrayList<>();
    for (int typeIndex = 0; typeIndex < sizes.size(); typeIndex++) {
      indexes.addAll(Collections.nCopies(sizes.get(typeIndex), typeIndex));
    }
    Collections.shuffle(indexes, new Random(seed));
    return indexes;
  }
}
