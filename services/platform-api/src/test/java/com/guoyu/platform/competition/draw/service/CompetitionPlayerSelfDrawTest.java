package com.guoyu.platform.competition.draw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CompetitionPlayerSelfDrawTest {

  @Test
  void picksOnlyRemainingNumber() {
    assertEquals(3, CompetitionPlayerSelfDraw.pick(4, List.of(1, 2, 4), new Random(1)));
  }

  @Test
  void selectionIsBoundedByRemainingNumbers() {
    assertEquals(2, CompetitionPlayerSelfDraw.pick(5, List.of(1, 3, 5), new FirstRandom()));
    assertEquals(4, CompetitionPlayerSelfDraw.pick(5, List.of(1, 3, 5), new LastRandom()));
  }

  @Test
  void rejectsDuplicateOrCompleteState() {
    assertThrows(IllegalArgumentException.class,
        () -> CompetitionPlayerSelfDraw.pick(3, List.of(1, 1), new Random()));
    assertThrows(IllegalStateException.class,
        () -> CompetitionPlayerSelfDraw.pick(2, List.of(1, 2), new Random()));
  }

  private static final class FirstRandom extends Random {
    @Override public int nextInt(int bound) { return 0; }
  }

  private static final class LastRandom extends Random {
    @Override public int nextInt(int bound) { return bound - 1; }
  }
}
