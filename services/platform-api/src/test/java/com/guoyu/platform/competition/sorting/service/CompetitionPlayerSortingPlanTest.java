package com.guoyu.platform.competition.sorting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompetitionPlayerSortingPlanTest {

  @Test
  void groupsFollowTheFirstCompanySeenAndKeepMemberOrder() {
    List<CompetitionPlayerSortingPlan.CompanyGroup> groups =
        CompetitionPlayerSortingPlan.groupByCompany(List.of(
            new CompetitionPlayerSortingPlan.MemberSeed("1", "甲公司"),
            new CompetitionPlayerSortingPlan.MemberSeed("2", "乙公司"),
            new CompetitionPlayerSortingPlan.MemberSeed("3", " 甲公司 "),
            new CompetitionPlayerSortingPlan.MemberSeed("4", " "),
            new CompetitionPlayerSortingPlan.MemberSeed("5", null)));

    assertEquals(List.of("甲公司", "乙公司", CompetitionPlayerSortingPlan.MISSING_ORG),
        groups.stream().map(CompetitionPlayerSortingPlan.CompanyGroup::orgName).toList());
    assertEquals(List.of("1", "3"), groups.get(0).profileIds());
    assertEquals(List.of("2"), groups.get(1).profileIds());
    assertEquals(List.of("4", "5"), groups.get(2).profileIds());
  }

  @Test
  void lastClickStillDrawsTheOnlyRemainingOrder() {
    assertEquals(2, CompetitionPlayerSortingPlan.drawUnusedOrder(3, java.util.Set.of(1, 3), new Random(1)));
  }

  @Test
  void drawAssignsOneDistinctCodeToEachGroup() {
    List<String> drawn = CompetitionPlayerSortingPlan.drawForGroups(
        2, List.of("703", "704", "715"), new Random(3));

    assertEquals(2, drawn.size());
    assertEquals(2, new HashSet<>(drawn).size());
    assertTrue(List.of("703", "704", "715").containsAll(drawn));
  }

  @Test
  void drawRejectsFewerCodesThanGroups() {
    assertThrows(IllegalArgumentException.class, () ->
        CompetitionPlayerSortingPlan.drawForGroups(3, List.of("B6", "A4"), new Random(1)));
  }

  @Test
  void workstationDrawTakesCompletedGroupsFirstAndLeavesTheRest() {
    List<Integer> groups = List.of(1, 2, 3, 4, 5, 6, 7, 8);
    Set<Integer> firstFour = Set.of(1, 2, 3, 4);

    CompetitionPlayerSortingPlan.WorkstationDrawChoice first =
        CompetitionPlayerSortingPlan.chooseWorkstationGroup(groups, firstFour, Set.of());
    assertEquals(CompetitionPlayerSortingPlan.WorkstationDrawGate.READY, first.gate());
    assertEquals(1, first.groupNo());

    CompetitionPlayerSortingPlan.WorkstationDrawChoice waiting =
        CompetitionPlayerSortingPlan.chooseWorkstationGroup(groups, firstFour, firstFour);
    assertEquals(CompetitionPlayerSortingPlan.WorkstationDrawGate.WAIT_FOR_REMAINING_GROUPS, waiting.gate());

    CompetitionPlayerSortingPlan.WorkstationDrawChoice remaining =
        CompetitionPlayerSortingPlan.chooseWorkstationGroup(groups, Set.of(1, 2, 3, 4, 5, 6, 7, 8), firstFour);
    assertEquals(CompetitionPlayerSortingPlan.WorkstationDrawGate.READY, remaining.gate());
    assertEquals(5, remaining.groupNo());

    CompetitionPlayerSortingPlan.WorkstationDrawChoice done =
        CompetitionPlayerSortingPlan.chooseWorkstationGroup(
            groups, Set.of(1, 2, 3, 4, 5, 6, 7, 8), Set.of(1, 2, 3, 4, 5, 6, 7, 8));
    assertEquals(CompetitionPlayerSortingPlan.WorkstationDrawGate.ALL_ASSIGNED, done.gate());
  }

  @Test
  void workstationDrawWaitsUntilAtLeastOneGroupFinishesOrdering() {
    CompetitionPlayerSortingPlan.WorkstationDrawChoice choice =
        CompetitionPlayerSortingPlan.chooseWorkstationGroup(List.of(1, 2), Set.of(), Set.of());

    assertEquals(CompetitionPlayerSortingPlan.WorkstationDrawGate.NEED_GROUP_ORDER, choice.gate());
  }

  @Test
  void repeatedDrawsCanReachEveryCode() {
    Random random = new Random(8);
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 40; i++) {
      seen.addAll(CompetitionPlayerSortingPlan.drawForGroups(1, List.of("703", "B6"), random));
    }
    assertEquals(Set.of("703", "B6"), seen);
  }

  @Test
  void shuffleGroupAppearanceRenumbersEveryGroupFromOne() {
    Map<Integer, Integer> mapping = CompetitionPlayerSortingPlan.shuffleGroupAppearance(
        List.of(2, 1, 2, 3), new Random(4));

    assertEquals(Set.of(1, 2, 3), new HashSet<>(mapping.keySet()));
    assertEquals(Set.of(1, 2, 3), new HashSet<>(mapping.values()));
  }
}
