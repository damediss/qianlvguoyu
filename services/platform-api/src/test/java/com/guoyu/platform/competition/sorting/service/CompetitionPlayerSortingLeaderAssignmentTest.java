package com.guoyu.platform.competition.sorting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingAssignmentVO;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class CompetitionPlayerSortingLeaderAssignmentTest {

  @Test
  void leaderOrderStaysInsideTheGroupAndCanLandOnAnySeat() {
    int[] sequence = {0, 2, 4, 1, 3};
    Random random = new Random() {
      private int index;

      @Override
      public int nextInt(int bound) {
        int value = sequence[index % sequence.length];
        index++;
        if (value >= bound) {
          throw new AssertionError("scripted value " + value + " is outside " + bound);
        }
        return value;
      }
    };

    assertEquals(1, CompetitionPlayerSortingApplicationService.leaderGroupOrder(5, random));
    assertEquals(3, CompetitionPlayerSortingApplicationService.leaderGroupOrder(5, random));
    assertEquals(5, CompetitionPlayerSortingApplicationService.leaderGroupOrder(5, random));

    Random singleSeat = new Random() {
      @Override
      public int nextInt(int bound) {
        assertEquals(1, bound);
        return 0;
      }
    };
    assertEquals(1, CompetitionPlayerSortingApplicationService.leaderGroupOrder(1, singleSeat));
  }

  @Test
  void repeatedDrawsCoverEverySeatInAGroup() {
    Random random = new Random(42);
    Set<Integer> seen = new HashSet<>();
    for (int i = 0; i < 200; i++) {
      int order = CompetitionPlayerSortingApplicationService.leaderGroupOrder(4, random);
      assertTrue(order >= 1 && order <= 4);
      seen.add(order);
    }
    assertEquals(Set.of(1, 2, 3, 4), seen);
  }

  @Test
  void historicalSnapshotWithoutLeaderStillReads() throws Exception {
    ObjectMapper mapper = new ObjectMapper()
        .registerModule(new ParameterNamesModule())
        .registerModule(new JavaTimeModule())
        .registerModule(new SimpleModule());
    String json = """
        [{"contestantProfileId":"1","contestantNo":"A01","name":"张三","mobile":"138****0000",
          "groupNo":1,"groupOrder":1,"workstation":"第一工位"}]
        """;

    List<CompetitionPlayerSortingAssignmentVO> assignments =
        mapper.readValue(json, new TypeReference<>() {});

    assertEquals(1, assignments.size());
    assertNull(assignments.get(0).groupLeader());
    assertNull(assignments.get(0).role());
    assertNull(assignments.get(0).orgName());
    assertEquals("张三", assignments.get(0).name());
  }

  @Test
  void rosterExportShowsExactlyOneLeaderPerGroup() throws Exception {
    List<CompetitionPlayerSortingAssignmentVO> assignments = List.of(
        assignment("1", "甲", 1, 1, true),
        assignment("2", "乙", 1, 2, false),
        assignment("3", "丙", 2, 1, false),
        assignment("4", "丁", 2, 2, true));

    byte[] workbookBytes = new CompetitionPlayerSortingWorkbookExportService()
        .export("电工赛段", assignments, 2);

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
      Sheet sheet = workbook.getSheet("实操出场名单");
      Row header = sheet.getRow(4);
      assertEquals("组内角色", header.getCell(4).getStringCellValue());
      assertEquals("组长", header.getCell(5).getStringCellValue());
      assertEquals("手机号", header.getCell(6).getStringCellValue());
      assertEquals("组长", sheet.getRow(5).getCell(5).getStringCellValue());
      assertEquals("", sheet.getRow(6).getCell(5).getStringCellValue());
      assertEquals("组长", sheet.getRow(8).getCell(5).getStringCellValue());
      assertEquals("甲", sheet.getRow(5).getCell(3).getStringCellValue());
      assertEquals("138****0001", sheet.getRow(5).getCell(6).getStringCellValue());
    }
  }

  private static CompetitionPlayerSortingAssignmentVO assignment(
      String profileId, String name, int groupNo, int groupOrder, boolean groupLeader) {
    return new CompetitionPlayerSortingAssignmentVO(
        profileId, "NO" + profileId, name, "138****000" + profileId,
        groupNo, groupOrder, "第一工位", groupLeader, null, null, null);
  }
}
