package com.guoyu.platform.competition.draw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.competition.draw.vo.CompetitionPlayerDrawSlot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CompetitionPlayerDrawSessionTest {

  @Test
  void firstRoundAssignsSequentialDrawOrderAfterShuffle() {
    List<CompetitionPlayerDrawSlot> ordered = CompetitionPlayerDrawSession.createDrawOrder(
        contestants("甲", "乙", "丙", "丁"), new FirstChoiceRandom());

    assertEquals(List.of("乙", "丙", "丁", "甲"),
        ordered.stream().map(CompetitionPlayerDrawSlot::name).toList());
    assertEquals(List.of(1, 2, 3, 4),
        ordered.stream().map(CompetitionPlayerDrawSlot::drawOrder).toList());
    assertTrue(ordered.stream().allMatch(slot -> slot.appearanceOrder() == null));
  }

  @Test
  void secondRoundDrawsContestantsInDrawOrderAndConsumesEachAppearanceNumberOnce() {
    List<CompetitionPlayerDrawSlot> slots = CompetitionPlayerDrawSession.createDrawOrder(
        contestants("甲", "乙", "丙", "丁"), new LastChoiceRandom());
    List<String> drawnNames = new ArrayList<>();
    List<Integer> appearanceOrders = new ArrayList<>();
    int playerCount = slots.size();

    for (int step = 0; step < playerCount; step++) {
      CompetitionPlayerDrawSession.DrawStep result =
          CompetitionPlayerDrawSession.drawNext(slots, new LastChoiceRandom());
      drawnNames.add(result.drawn().name());
      appearanceOrders.add(result.drawn().appearanceOrder());
      slots = result.slots();
      assertEquals(step == playerCount - 1, result.complete());
    }

    List<CompetitionPlayerDrawSlot> finished = slots;
    assertEquals(List.of("甲", "乙", "丙", "丁"), drawnNames);
    assertEquals(List.of(4, 3, 2, 1), appearanceOrders);
    assertEquals(4, new HashSet<>(appearanceOrders).size());
    assertThrows(IllegalStateException.class,
        () -> CompetitionPlayerDrawSession.drawNext(finished, new LastChoiceRandom()));
  }

  @Test
  void secondRoundAlwaysDrawsTheEarliestUndrawnContestant() {
    List<CompetitionPlayerDrawSlot> slots = List.of(
        slot("c", "丙", 3, null),
        slot("a", "甲", 1, null),
        slot("b", "乙", 2, 2));

    CompetitionPlayerDrawSession.DrawStep result =
        CompetitionPlayerDrawSession.drawNext(slots, new FirstChoiceRandom());

    assertEquals("甲", result.drawn().name());
    assertEquals(1, result.drawn().drawOrder());
    assertEquals(1, result.drawn().appearanceOrder());
    Integer yi = result.slots().stream()
        .filter(slot -> "乙".equals(slot.name()))
        .findFirst()
        .orElseThrow()
        .appearanceOrder();
    assertEquals(2, yi);
    assertNull(result.slots().stream()
        .filter(slot -> "丙".equals(slot.name()))
        .findFirst()
        .orElseThrow()
        .appearanceOrder());
  }

  @Test
  void emptyContestantListIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> CompetitionPlayerDrawSession.createDrawOrder(List.of(), new FirstChoiceRandom()));
  }

  private static List<CompetitionPlayerDrawSlot> contestants(String... names) {
    List<CompetitionPlayerDrawSlot> slots = new ArrayList<>();
    for (int index = 0; index < names.length; index++) {
      slots.add(slot("p" + index, names[index], 0, null));
    }
    return slots;
  }

  private static CompetitionPlayerDrawSlot slot(
      String id, String name, int drawOrder, Integer appearanceOrder) {
    return new CompetitionPlayerDrawSlot(id, "NO-" + id, name, "企业", null, drawOrder, appearanceOrder);
  }

  /** nextInt always returns 0. Shuffle then moves the list to 乙、丙、丁、甲, and each draw takes the smallest free number. */
  private static final class FirstChoiceRandom extends Random {
    @Override
    public int nextInt(int bound) {
      return 0;
    }
  }

  /** nextInt always returns the last legal index, so shuffle keeps order and the draw takes the largest free number. */
  private static final class LastChoiceRandom extends Random {
    @Override
    public int nextInt(int bound) {
      return bound - 1;
    }
  }
}
