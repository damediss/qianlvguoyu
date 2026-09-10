package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompetitionQuestionWorkbookParserTest {

  private final CompetitionQuestionWorkbookParser parser = new CompetitionQuestionWorkbookParser();

  @TempDir
  Path tempDir;

  @Test
  void parsesTheoryAThroughHAndFillAnswers() throws Exception {
    Path file = writeTheoryWorkbook();

    CompetitionQuestionWorkbookParser.ParsedWorkbook result = parser.parse(file, "theory");

    assertThat(result.questions()).hasSize(2);
    var choice = result.questions().get(0);
    assertThat(choice.questionType()).isEqualTo("multi");
    assertThat(choice.referenceAnswer()).isEqualTo("AC H");
    assertThat(choice.optionsJson()).contains("\"A\"").contains("\"H\"");
    var fill = result.questions().get(1);
    assertThat(fill.questionType()).isEqualTo("fill");
    assertThat(fill.fillAnswer()).isEqualTo("安全帽");
    assertThat(fill.optionsJson()).isNull();
  }

  @Test
  void rejectsTheoryAnswerThatDoesNotHaveAnOption() throws Exception {
    Path file = tempDir.resolve("invalid-theory.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("理论题库");
      String[] headers = {"序号", "题型", "题干", "参考答案", "选项A"};
      var header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue(1);
      row.createCell(1).setCellValue("单选题");
      row.createCell(2).setCellValue("题干");
      row.createCell(3).setCellValue("B");
      row.createCell(4).setCellValue("只有A");
      try (var output = Files.newOutputStream(file)) {
        workbook.write(output);
      }
    }

    assertThatThrownBy(() -> parser.parse(file, "theory"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("答案选项 B 没有对应选项内容");
  }

  @Test
  void groupsPracticalRowsAndKeepsEveryScoringConstraint() throws Exception {
    Path file = tempDir.resolve("practical.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("实操题库");
      String[] headers = {"序号", "题干", "操作内容", "考生准备", "设备准备",
          "评分项序号", "评分要素", "评分标准", "最大分值", "最大扣分", "扣分步长"};
      var header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      writePracticalRow(sheet.createRow(1), 1, "铝材焊接", "按工艺完成焊接", "防护用品",
          "焊机", 1, "焊缝外观", "无明显缺陷", 6, 2, 1);
      writePracticalRow(sheet.createRow(2), 1, "", "", "", "", 2, "尺寸", "符合图纸", 4, 4, 2);
      try (var output = Files.newOutputStream(file)) {
        workbook.write(output);
      }
    }

    var result = parser.parse(file, "practical");

    assertThat(result.questions()).hasSize(1);
    var question = result.questions().get(0);
    assertThat(question.questionType()).isEqualTo("practical");
    assertThat(question.scoreItems()).extracting(
        CompetitionQuestionWorkbookParser.ParsedScoreItem::itemName)
        .containsExactly("焊缝外观", "尺寸");
    assertThat(question.scoreItems()).extracting(
        CompetitionQuestionWorkbookParser.ParsedScoreItem::deductionStep)
        .containsExactly(new BigDecimal("1"), new BigDecimal("2"));
  }

  private Path writeTheoryWorkbook() throws Exception {
    Path file = tempDir.resolve("theory.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("理论题库");
      String[] headers = {"序号", "题型", "题干", "参考答案", "回答",
          "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "选项G", "选项H"};
      var header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      var choice = sheet.createRow(1);
      choice.createCell(0).setCellValue(1);
      choice.createCell(1).setCellValue("多选题");
      choice.createCell(2).setCellValue("选择所有正确选项");
      choice.createCell(3).setCellValue("AC H");
      for (int i = 0; i < 8; i++) choice.createCell(5 + i).setCellValue("选项" + (char) ('A' + i));
      var fill = sheet.createRow(2);
      fill.createCell(0).setCellValue(2);
      fill.createCell(1).setCellValue("填空题");
      fill.createCell(2).setCellValue("作业时应佩戴什么");
      fill.createCell(4).setCellValue("安全帽");
      try (var output = Files.newOutputStream(file)) {
        workbook.write(output);
      }
    }
    return file;
  }

  private void writePracticalRow(
      org.apache.poi.ss.usermodel.Row row,
      int sequence,
      String stem,
       String operation,
      String student,
      String equipment,
      int itemNo,
      String itemName,
      String criterion,
      int maxScore,
      int maxDeduction,
      int step) {
    row.createCell(0).setCellValue(sequence);
    row.createCell(1).setCellValue(stem);
    if (!operation.isBlank()) row.createCell(2).setCellValue(operation);
    if (!student.isBlank()) row.createCell(3).setCellValue(student);
    if (!equipment.isBlank()) row.createCell(4).setCellValue(equipment);
    row.createCell(5).setCellValue(itemNo);
    row.createCell(6).setCellValue(itemName);
    row.createCell(7).setCellValue(criterion);
    row.createCell(8).setCellValue(maxScore);
    row.createCell(9).setCellValue(maxDeduction);
    row.createCell(10).setCellValue(step);
  }
}
