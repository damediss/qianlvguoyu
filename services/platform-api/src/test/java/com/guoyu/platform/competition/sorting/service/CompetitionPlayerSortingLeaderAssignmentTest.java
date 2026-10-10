package com.guoyu.platform.competition.sorting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingAssignmentVO;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class CompetitionPlayerSortingLeaderAssignmentTest {

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
  void rosterExportKeepsOrderNumbersAndOmitsLeaderColumn() throws Exception {
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
      assertEquals("手机号", header.getCell(5).getStringCellValue());
      assertEquals("参赛公司", header.getCell(6).getStringCellValue());
      assertEquals("子工位", header.getCell(7).getStringCellValue());
      assertEquals(1D, sheet.getRow(5).getCell(1).getNumericCellValue());
      assertEquals("甲", sheet.getRow(5).getCell(3).getStringCellValue());
      assertEquals("138****0001", sheet.getRow(5).getCell(5).getStringCellValue());
      assertEquals(2D, sheet.getRow(6).getCell(1).getNumericCellValue());
      for (int rowIndex = 4; rowIndex <= 8; rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }
        for (int column = 0; column <= 8; column++) {
          Cell cell = row.getCell(column);
          if (cell == null || cell.getCellType() != CellType.STRING) {
            continue;
          }
          assertEquals(false, "组长".equals(cell.getStringCellValue()),
              "row " + rowIndex + " column " + column);
        }
      }
    }
  }

  private static CompetitionPlayerSortingAssignmentVO assignment(
      String profileId, String name, int groupNo, int groupOrder, boolean groupLeader) {
    return new CompetitionPlayerSortingAssignmentVO(
        profileId, "NO" + profileId, name, "138****000" + profileId,
        groupNo, groupOrder, "第一工位", groupLeader, null, null, null, null);
  }
}
