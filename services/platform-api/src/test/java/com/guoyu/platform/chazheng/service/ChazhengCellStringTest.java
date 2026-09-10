package com.guoyu.platform.chazheng.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * 查证导入单元格读取：数值/公式/布尔/空白单元格不得抛 IllegalStateException。
 * 生产事故：用户名单身份证号列被录成数值格式，getStringCellValue 硬读导致导入接口 500。
 */
class ChazhengCellStringTest {

  @Test
  void readCellsByType() throws Exception {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row row = sheet.createRow(0);

      row.createCell(0).setCellValue(" 张三 ");                    // 文本带空格
      row.createCell(1).setCellValue(452301199001012345.0);       // 数值（已按 double 精度存储）
      row.createCell(2).setCellValue(60.5);                       // 小数
      row.createCell(3).setCellValue(true);                       // 布尔
      row.createCell(4).setCellFormula("CONCATENATE(\"AB\",\"C\")"); // 公式（无缓存值）
      // cell 5 留空

      assertEquals("张三", ChazhengApplicationService.getCellString(row, 0));
      // 数值按整数还原：不带 .0、不科学计数法
      String idCard = ChazhengApplicationService.getCellString(row, 1);
      // 18 位数字超 double 精确范围，只能还原 Excel 实际存储的值（末几位已失真——源文件层面的精度损失）
      assertEquals("452301199001012350", idCard, "数值单元格应还原为整数字符串（double 最短回环表示）");
      assertEquals("60.5", ChazhengApplicationService.getCellString(row, 2));
      assertEquals("true", ChazhengApplicationService.getCellString(row, 3));
      // 公式无缓存值时不崩，尽力返回
      ChazhengApplicationService.getCellString(row, 4);
      assertEquals("", ChazhengApplicationService.getCellString(row, 5));
      assertEquals("", ChazhengApplicationService.getCellString(row, 99), "越界列返回空串");
    }
  }

  @Test
  void numericIdCardNotScientific() throws Exception {
    try (Workbook wb = new XSSFWorkbook()) {
      Row row = wb.createSheet().createRow(0);
      row.createCell(0).setCellValue(1.1e17);
      String v = ChazhengApplicationService.getCellString(row, 0);
      assertEquals("110000000000000000", v, "大数值不得出现 E 计数法");
    }
  }

  /** 宽表（认定名单）：身份证号在 AO 列附近，按表头定位而非固定 A/B */
  @Test
  void detectColumnsByHeaderInWideSheet() throws Exception {
    try (Workbook wb = new XSSFWorkbook()) {
      Row header = wb.createSheet().createRow(0);
      header.createCell(0).setCellValue("序号");
      header.createCell(1).setCellValue("工号");
      header.createCell(2).setCellValue("姓名");
      header.createCell(3).setCellValue("单位部门");
      header.createCell(40).setCellValue("身份证号");   // AO 列
      header.createCell(41).setCellValue("证件地址");

      int[] cols = ChazhengApplicationService.detectNameIdColumns(header);
      assertEquals(2, cols[0], "姓名列应为 C 列");
      assertEquals(40, cols[1], "身份证列应为 AO 列");
    }
  }

  /** 「证件号码」表头也应识别为身份证列 */
  @Test
  void detectColumnsByCertNoHeader() throws Exception {
    try (Workbook wb = new XSSFWorkbook()) {
      Row header = wb.createSheet().createRow(0);
      header.createCell(0).setCellValue("姓名");
      header.createCell(1).setCellValue("证件号码");
      int[] cols = ChazhengApplicationService.detectNameIdColumns(header);
      assertEquals(0, cols[0]);
      assertEquals(1, cols[1]);
    }
  }

  /** 无匹配表头时回退固定模板 A=姓名 / B=身份证号 */
  @Test
  void detectColumnsFallbackToFixedTemplate() throws Exception {
    try (Workbook wb = new XSSFWorkbook()) {
      Row header = wb.createSheet().createRow(0);
      header.createCell(0).setCellValue("编号");
      header.createCell(1).setCellValue("备注");
      int[] cols = ChazhengApplicationService.detectNameIdColumns(header);
      assertEquals(0, cols[0]);
      assertEquals(1, cols[1]);
    }
    // 完全无表头行
    try (Workbook wb = new XSSFWorkbook()) {
      int[] cols = ChazhengApplicationService.detectNameIdColumns(null);
      assertEquals(0, cols[0]);
      assertEquals(1, cols[1]);
    }
  }
}
