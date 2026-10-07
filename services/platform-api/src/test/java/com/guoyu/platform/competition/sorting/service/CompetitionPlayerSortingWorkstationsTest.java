package com.guoyu.platform.competition.sorting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CompetitionPlayerSortingWorkstationsTest {

  @Test
  void onePlayerPerWorkstationWhenStationCountEqualsGroupSize() {
    List<String> stations = CompetitionPlayerSortingWorkstations.assignGroup(9, 9);

    assertEquals(List.of(
        "第一工位", "第二工位", "第三工位", "第四工位", "第五工位",
        "第六工位", "第七工位", "第八工位", "第九工位"), stations);
  }

  @Test
  void everyPlayerSharesTheOnlyWorkstation() {
    List<String> stations = CompetitionPlayerSortingWorkstations.assignGroup(9, 1);

    assertEquals(9, stations.size());
    assertTrue(stations.stream().allMatch("第一工位"::equals));
  }

  @Test
  void unevenStationCountGivesEarlierStationsOneExtraPlayer() {
    List<String> stations = CompetitionPlayerSortingWorkstations.assignGroup(9, 4);
    Map<String, Long> counts = stations.stream()
        .collect(Collectors.groupingBy(station -> station, Collectors.counting()));

    assertEquals(List.of(
        "第一工位", "第二工位", "第三工位", "第四工位",
        "第一工位", "第二工位", "第三工位", "第四工位",
        "第一工位"), stations);
    assertEquals(3L, counts.get("第一工位"));
    assertEquals(2L, counts.get("第二工位"));
    assertEquals(2L, counts.get("第三工位"));
    assertEquals(2L, counts.get("第四工位"));
  }

  @Test
  void smallerActualGroupLeavesUnusedStationsEmpty() {
    List<String> stations = CompetitionPlayerSortingWorkstations.assignGroup(7, 9);

    assertEquals(List.of(
        "第一工位", "第二工位", "第三工位", "第四工位", "第五工位", "第六工位", "第七工位"),
        stations);
  }

  @Test
  void chineseWorkstationNumbersCoverTensHundredsAndThousands() {
    assertEquals("十", CompetitionPlayerSortingWorkstations.chinese(10));
    assertEquals("十一", CompetitionPlayerSortingWorkstations.chinese(11));
    assertEquals("二十", CompetitionPlayerSortingWorkstations.chinese(20));
    assertEquals("二十一", CompetitionPlayerSortingWorkstations.chinese(21));
    assertEquals("一百", CompetitionPlayerSortingWorkstations.chinese(100));
    assertEquals("一百零一", CompetitionPlayerSortingWorkstations.chinese(101));
    assertEquals("一百一十", CompetitionPlayerSortingWorkstations.chinese(110));
    assertEquals("一千零一", CompetitionPlayerSortingWorkstations.chinese(1001));
    assertEquals("一千零一十", CompetitionPlayerSortingWorkstations.chinese(1010));
    assertEquals("二千一百零一", CompetitionPlayerSortingWorkstations.chinese(2101));
    assertEquals("五千", CompetitionPlayerSortingWorkstations.chinese(5000));
  }
}
