package com.guoyu.platform.exam.theorygen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** 试题表导出参数口径（指引 §理论知识试题的基本参数：代码 A-A-A-001、难度/一致性中文、判断题对/错） */
class TheoryGenExportServiceTest {

  private TheoryGenDraftEntity draft(String questionType, String answer, Integer difficulty,
      Integer consistency) {
    TheoryGenDraftEntity d = new TheoryGenDraftEntity();
    d.setNodeId(4L);
    d.setBlueprintId(100L);
    d.setQuestionType(questionType);
    d.setAnswer(answer);
    d.setDifficulty(difficulty);
    d.setConsistency(consistency);
    d.setContent("题干");
    return d;
  }

  private TheoryBlueprintNodeEntity node(long id, long parentId, int level, String code, String name) {
    TheoryBlueprintNodeEntity n = new TheoryBlueprintNodeEntity();
    n.setId(id);
    n.setParentId(parentId);
    n.setNodeLevel(level);
    n.setCode(code);
    n.setName(name);
    n.setImportance("X");
    return n;
  }

  @Test
  void exportFollowsGuidelineParams() throws Exception {
    TheoryGenDraftService draftService = mock(TheoryGenDraftService.class);
    TheoryBlueprintApplicationService blueprintService = mock(TheoryBlueprintApplicationService.class);
    // 层级链：叶子(001) ← 三级(A) ← 二级(A) ← 一级(A)
    when(blueprintService.findBlueprintNodes(any(), any())).thenReturn(List.of(
        node(1L, 0L, 1, "A", "基本要求"),
        node(2L, 1L, 2, "A", "职业道德"),
        node(3L, 2L, 3, "A", "职业道德基本知识"),
        node(4L, 3L, 4, "001", "职业道德基本内涵")));
    when(draftService.listAll(any(), any())).thenReturn(List.of(
        draft("judge", "√", 2, 5),
        draft("single", "B", 4, 1)));

    TheoryGenExportService service = new TheoryGenExportService(draftService, blueprintService);
    byte[] bytes = service.exportBatch(1L, 300L);

    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = wb.getSheet("试题表");
      Row head = sheet.getRow(0);
      assertEquals("一级代码名称", head.getCell(1).getStringCellValue());
      assertEquals("三级代码名称", head.getCell(3).getStringCellValue());
      assertEquals("考评点名称", head.getCell(4).getStringCellValue());
      assertEquals("题型", head.getCell(6).getStringCellValue());
      assertEquals("难度", head.getCell(14).getStringCellValue());

      Row judge = sheet.getRow(1);
      assertEquals("A 基本要求", judge.getCell(1).getStringCellValue());
      assertEquals("A 职业道德", judge.getCell(2).getStringCellValue());
      assertEquals("A 职业道德基本知识", judge.getCell(3).getStringCellValue());
      assertEquals("职业道德基本内涵", judge.getCell(4).getStringCellValue());
      assertEquals("判断题", judge.getCell(6).getStringCellValue());
      assertEquals("对", judge.getCell(13).getStringCellValue());
      assertEquals("较易", judge.getCell(14).getStringCellValue());
      assertEquals("好", judge.getCell(15).getStringCellValue());

      Row single = sheet.getRow(2);
      assertEquals("A 基本要求", single.getCell(1).getStringCellValue());
      assertEquals("单选题", single.getCell(6).getStringCellValue());
      assertEquals("B", single.getCell(13).getStringCellValue());
      assertEquals("较难", single.getCell(14).getStringCellValue());
      assertEquals("差", single.getCell(15).getStringCellValue());
    }
  }
}
