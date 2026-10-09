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
  void exportGroupsByTypeWithoutContestantNumber() throws Exception {
    byte[] bytes = new CompetitionPlayerClassifyWorkbookExportService().export("实操赛段", List.of(
        new CompetitionPlayerClassifyAssignmentVO("2", "NO-2", "李四", "乙公司", 2, "突发事件异常处理", 1),
        new CompetitionPlayerClassifyAssignmentVO("1", "NO-1", "张三", "甲公司", 1, "焦粒启槽", 1)));
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row header = sheet.getRow(1);
      assertEquals("分类", header.getCell(0).getStringCellValue());
      assertEquals("类型内顺序", header.getCell(1).getStringCellValue());
      assertEquals("姓名", header.getCell(2).getStringCellValue());
      assertEquals("参赛公司", header.getCell(3).getStringCellValue());
      assertFalse(header.getCell(0).getStringCellValue().contains("选手编号"));
      assertEquals(null, header.getCell(4));
      Row first = sheet.getRow(2);
      assertEquals("焦粒启槽", first.getCell(0).getStringCellValue());
      assertEquals(1, (int) first.getCell(1).getNumericCellValue());
      assertEquals("张三", first.getCell(2).getStringCellValue());
      Row second = sheet.getRow(3);
      assertEquals("突发事件异常处理", second.getCell(0).getStringCellValue());
      assertEquals("李四", second.getCell(2).getStringCellValue());
    }
  }
}
