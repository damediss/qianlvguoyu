package com.guoyu.platform.competition.classify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.guoyu.platform.competition.classify.vo.CompetitionPlayerClassifyAssignmentVO;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class CompetitionPlayerClassifyWorkbookExportServiceTest {

  @Test
  void exportFollowsGlobalOrderAndInterleavesTypes() throws Exception {
    byte[] bytes = new CompetitionPlayerClassifyWorkbookExportService().export("实操赛段", List.of(
        new CompetitionPlayerClassifyAssignmentVO("4", "NO-4", "赵六", "丁公司", 1, "焦粒启槽", 4),
        new CompetitionPlayerClassifyAssignmentVO("2", "NO-2", "李四", "乙公司", 2, "突发事件异常处理", 2),
        new CompetitionPlayerClassifyAssignmentVO("3", "NO-3", "王五", "丙公司", 2, "突发事件异常处理", 3),
        new CompetitionPlayerClassifyAssignmentVO("1", "NO-1", "张三", "甲公司", 1, "焦粒启槽", 1)));
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row header = sheet.getRow(1);
      assertEquals("顺序", header.getCell(0).getStringCellValue());
      assertEquals("姓名", header.getCell(1).getStringCellValue());
      assertEquals("分类", header.getCell(2).getStringCellValue());
      assertEquals("参赛公司", header.getCell(3).getStringCellValue());
      assertFalse(header.getCell(0).getStringCellValue().contains("选手编号"));
      assertEquals(null, header.getCell(4));

      assertRow(sheet.getRow(2), 1, "张三", "焦粒启槽");
      assertRow(sheet.getRow(3), 2, "李四", "突发事件异常处理");
      assertRow(sheet.getRow(4), 3, "王五", "突发事件异常处理");
      assertRow(sheet.getRow(5), 4, "赵六", "焦粒启槽");
    }
  }

  private void assertRow(Row row, int order, String name, String typeName) {
    assertEquals(order, (int) row.getCell(0).getNumericCellValue());
    assertEquals(name, row.getCell(1).getStringCellValue());
    assertEquals(typeName, row.getCell(2).getStringCellValue());
  }
}
