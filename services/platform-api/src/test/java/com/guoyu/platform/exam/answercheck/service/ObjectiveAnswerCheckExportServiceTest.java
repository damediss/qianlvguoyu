package com.guoyu.platform.exam.answercheck.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckBatchEntity;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** 导出（方案 §14.1 / §16.1-H）：快照导出、两 Sheet、百分比数值格式、空结果与 2000 行。 */
class ObjectiveAnswerCheckExportServiceTest {

  private final ObjectiveAnswerCheckExportService service =
      new ObjectiveAnswerCheckExportService(new ObjectMapper());

  private ObjectiveAnswerCheckBatchEntity batch() {
    ObjectiveAnswerCheckBatchEntity batch = new ObjectiveAnswerCheckBatchEntity();
    batch.setId(2100000000000012345L);
    batch.setModelId("ateve-jev-v1");
    batch.setPromptVersion("objective-answer-v1");
    batch.setConfidenceThreshold(new BigDecimal("0.800"));
    batch.setMarginThreshold(new BigDecimal("0.200"));
    batch.setTotal(1);
    batch.setDone(1);
    batch.setConsistentHigh(1);
    batch.setMismatchHigh(0);
    batch.setLowConfidence(0);
    batch.setInvalid(0);
    batch.setFailed(0);
    batch.setContentSuspect(0);
    batch.setContentInvalid(0);
    batch.setExpectedCalls(1);
    batch.setCallCount(1);
    batch.setInputTokensExact(10L);
    batch.setInputTokensEstimated(0L);
    batch.setUsageEstimatedCalls(0);
    batch.setOutputTokens(0L);
    batch.setCreatedBy(1L);
    batch.setFilterJson("{\"workTypeTitle\":\"铝电解工\"}");
    return batch;
  }

  private ObjectiveAnswerCheckItemEntity item(int index) {
    ObjectiveAnswerCheckItemEntity item = new ObjectiveAnswerCheckItemEntity();
    item.setId(2100000000000100000L + index);
    item.setDetailId(index + 1);
    item.setQuestionType(index % 3 == 0 ? "single" : index % 3 == 1 ? "multi" : "judge");
    item.setStemText("题干" + index);
    item.setOptionSnapshotJson("[{\"optionId\":\"opt_0\",\"label\":\"A\",\"text\":\"甲\"},"
        + "{\"optionId\":\"opt_1\",\"label\":\"B\",\"text\":\"乙\"}]");
    item.setOriginalAnswerKeys("opt_0");
    item.setAiAnswerKeys(index % 2 == 0 ? "opt_0" : "opt_1");
    item.setResultCode(index % 2 == 0 ? "consistent_high" : "low_confidence");
    item.setConsistent(index % 2 == 0 ? 1 : 0);
    item.setContentQuality(index % 5 == 0 ? "suspect" : "clean");
    item.setContentWarningsJson("[]");
    item.setReferenceConfidence(new BigDecimal("0.85000"));
    item.setAiConfidence(new BigDecimal("0.91000"));
    item.setMinMargin(new BigDecimal("0.78000"));
    item.setReviewStatus("unreviewed");
    item.setErrorMessage(null);
    return item;
  }

  private XSSFWorkbook read(ExportResultDTO result) throws Exception {
    assertNotNull(result.data());
    return new XSSFWorkbook(new java.io.ByteArrayInputStream(result.data()));
  }

  @Test
  void exportHasTwoSheetsAndPercentNumericCells() throws Exception {
    ExportResultDTO result = service.export(batch(), List.of(item(0)));
    assertTrue(result.filename().startsWith("客观题答案AI检查_"));
    assertTrue(result.filename().endsWith(".xlsx"));
    assertTrue(result.filename().contains("00012345"), "文件名包含批次后 8 位");

    try (XSSFWorkbook workbook = read(result)) {
      assertEquals(2, workbook.getNumberOfSheets());
      assertEquals("批次信息", workbook.getSheetName(0));
      assertEquals("检查结果", workbook.getSheetName(1));
      Sheet sheet = workbook.getSheetAt(1);
      Row header = sheet.getRow(0);
      assertEquals(30, header.getLastCellNum());
      Row row = sheet.getRow(1);
      Cell reference = row.getCell(13);
      assertEquals(CellType.NUMERIC, reference.getCellType());
      assertEquals(0.85, reference.getNumericCellValue(), 1e-9);
      assertTrue(reference.getCellStyle().getDataFormatString().contains("%"));
      assertEquals("高置信一致", row.getCell(17).getStringCellValue());
      assertEquals("格式疑似异常", row.getCell(18).getStringCellValue());
    }
  }

  @Test
  void emptyResultStillWritesHeaderRow() throws Exception {
    ExportResultDTO result = service.export(batch(), List.of());
    try (XSSFWorkbook workbook = read(result)) {
      Sheet sheet = workbook.getSheetAt(1);
      assertEquals(0, sheet.getLastRowNum());
      assertEquals("题干", sheet.getRow(0).getCell(8).getStringCellValue());
    }
  }

  @Test
  void twoThousandRowsExportWithoutNpe() throws Exception {
    List<ObjectiveAnswerCheckItemEntity> items = new ArrayList<>();
    for (int i = 0; i < 2000; i++) {
      items.add(item(i));
    }
    ExportResultDTO result = service.export(batch(), items);
    try (XSSFWorkbook workbook = read(result)) {
      Sheet sheet = workbook.getSheetAt(1);
      assertEquals(2000, sheet.getLastRowNum());
      Row last = sheet.getRow(2000);
      assertNotNull(last);
      assertEquals("题干1999", last.getCell(8).getStringCellValue());
    }
  }
}
