package com.guoyu.platform.exam.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * XLSX 单元格读取（方案 §14.4.2 / §16.1-A）：
 * 普通数值禁止 int 截断，日期/百分比/自定义格式保留显示语义，公式与错误值按类型分派。
 */
class ExcelCellTextReaderTest {

  private XSSFWorkbook workbook;
  private Sheet sheet;
  private ExcelCellTextReader reader;

  @BeforeEach
  void setUp() {
    workbook = new XSSFWorkbook();
    sheet = workbook.createSheet("Sheet1");
    reader = new ExcelCellTextReader(workbook);
  }

  @AfterEach
  void tearDown() throws Exception {
    workbook.close();
  }

  private ExcelCellValue read(int rowIndex, int col) {
    Row row = sheet.getRow(rowIndex);
    return reader.read(row == null ? null : row.getCell(col), "Sheet1", rowIndex + 1, col + 1,
        "字段");
  }

  private Cell cell(int rowIndex, int col) {
    Row row = sheet.getRow(rowIndex) == null ? sheet.createRow(rowIndex) : sheet.getRow(rowIndex);
    return row.getCell(col) == null ? row.createCell(col) : row.getCell(col);
  }

  @Test
  void generalNumberKeepsDecimalWithoutIntTruncation() {
    cell(0, 0).setCellValue(0.5);
    cell(0, 1).setCellValue(1.5);
    cell(0, 2).setCellValue(0);
    cell(0, 3).setCellValue(-3.25);

    assertEquals("0.5", read(0, 0).text());
    assertEquals("1.5", read(0, 1).text());
    assertEquals("0", read(0, 2).text());
    assertEquals("-3.25", read(0, 3).text());
    assertFalse(read(0, 0).hasError());
  }

  @Test
  void scientificFormatRestoresPlainDecimal() {
    Cell cell = cell(0, 0);
    CellStyle style = workbook.createCellStyle();
    style.setDataFormat(workbook.createDataFormat().getFormat("0.00E+00"));
    cell.setCellStyle(style);
    cell.setCellValue(1200);

    assertEquals("1200", read(0, 0).text());
  }

  @Test
  void customNumberFormatKeepsDisplaySemantics() {
    Cell half = cell(0, 0);
    CellStyle twoDecimals = workbook.createCellStyle();
    twoDecimals.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
    half.setCellStyle(twoDecimals);
    half.setCellValue(0.5);

    Cell thousands = cell(0, 1);
    CellStyle grouped = workbook.createCellStyle();
    grouped.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
    thousands.setCellStyle(grouped);
    thousands.setCellValue(12345);

    assertEquals("0.50", read(0, 0).text());
    assertEquals("12,345", read(0, 1).text());
  }

  @Test
  void percentAndDateUseFormatterNotRawSerial() {
    Cell percent = cell(0, 0);
    CellStyle percentStyle = workbook.createCellStyle();
    percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0%"));
    percent.setCellStyle(percentStyle);
    percent.setCellValue(0.25);

    Cell date = cell(0, 1);
    CellStyle dateStyle = workbook.createCellStyle();
    dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
    date.setCellStyle(dateStyle);
    Date value = Date.from(
        LocalDate.of(2026, 10, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
    date.setCellValue(value);

    assertEquals("25%", read(0, 0).text());
    assertEquals("2026-10-28", read(0, 1).text());
  }

  @Test
  void booleanBlankAndStringDispatch() {
    cell(0, 0).setCellValue(true);
    cell(0, 1).setBlank();
    cell(0, 2).setCellValue(" 文本 ");

    assertEquals("true", read(0, 0).text());
    assertEquals("", read(0, 1).text());
    assertEquals("文本", read(0, 2).text());
  }

  @Test
  void formulaUsesCachedResult() {
    Cell cell = cell(0, 0);
    cell.setCellFormula("1+1");
    CreationHelper helper = workbook.getCreationHelper();
    helper.createFormulaEvaluator().evaluateFormulaCell(cell);

    assertEquals("2", read(0, 0).text());
    assertFalse(read(0, 0).hasError());
  }

  @Test
  void formulaWithoutCachedValueIsHardError() {
    Cell cell = cell(0, 0);
    cell.setCellFormula("SUM(A1:A2)");

    ExcelCellValue value = read(0, 0);
    assertTrue(value.hasError());
    assertTrue(value.issues().stream()
        .anyMatch(i -> ExcelCellTextReader.RULE_FORMULA_CACHE_MISSING.equals(i.ruleCode())));
  }

  @Test
  void errorLiteralAndNonFiniteAreHardErrors() {
    cell(0, 0).setCellErrorValue(FormulaError.REF.getCode());
    cell(0, 1).setCellValue(Double.NaN);
    cell(0, 2).setCellValue(Double.POSITIVE_INFINITY);

    List<ExcelCellValue> values = List.of(read(0, 0), read(0, 1), read(0, 2));
    for (ExcelCellValue value : values) {
      assertTrue(value.hasError(), "应为硬错误: " + value);
    }
    assertTrue(values.get(0).issues().stream()
        .anyMatch(i -> "#REF!".equals(i.matchedText())));
  }

  @Test
  void moreThanFifteenSignificantDigitsWarnsWithoutClaimingRecovery() {
    cell(0, 0).setCellValue(12345678901234567d);

    ExcelCellValue value = read(0, 0);
    assertFalse(value.hasError());
    assertTrue(value.issues().stream()
        .anyMatch(i -> ExcelCellTextReader.RULE_NUMERIC_PRECISION_WARNING.equals(i.ruleCode())
            && QuestionImportIssue.SEVERITY_WARNING.equals(i.severity())));
    assertEquals(value.text(), value.issues().get(0).matchedText());
  }
}
