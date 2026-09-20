package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompetitionBlueprintWorkbookParserTest {

  private final CompetitionBlueprintWorkbookParser parser = new CompetitionBlueprintWorkbookParser();

  @TempDir
  Path tempDir;

  @Test
  void parsesTheoryMergedHierarchyAndPercentageWeights() throws Exception {
    Path file = tempDir.resolve("theory-blueprint.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("理论细目表");
      sheet.createRow(0).createCell(0).setCellValue("氧化铝焙烧工三级理论细目表");
      String[] headers = {"考评范围一级", "一级权重", "考评范围二级", "二级权重",
          "考评范围三级", "三级权重", "相关知识", "考评点代码", "考评点名称", "重要程度"};
      var header = sheet.createRow(1);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      writeTheoryRow(sheet.createRow(2), "A\n基础知识", "50%", "A-1\n安全操作", "100%",
          "A-1-1\n劳动保护", "100%", "PPE", "A-1-1-1", "正确佩戴防护用品", "X");
      writeTheoryRow(sheet.createRow(3), null, null, null, null, null, null, null,
          "A-1-1-2", "安全标识", "Y");
      writeTheoryRow(sheet.createRow(4), "B\n专业知识", "50%", "B-1\n工艺", "100%",
          "B-1-1\n焙烧工艺", "100%", "工艺参数", "B-1-1-1", "温度控制", "Z");
      sheet.addMergedRegion(new CellRangeAddress(2, 3, 0, 0));
      sheet.addMergedRegion(new CellRangeAddress(2, 3, 2, 2));
      sheet.addMergedRegion(new CellRangeAddress(2, 3, 4, 4));
      try (var output = Files.newOutputStream(file)) { workbook.write(output); }
    }

    var parsed = parser.parse(file, "theory");

    assertThat(parsed.rows()).hasSize(9);
    assertThat(parsed.rows()).extracting(CompetitionBlueprintWorkbookParser.ParsedNode::nodeLevel)
        .containsExactly(1, 2, 3, 4, 4, 1, 2, 3, 4);
    assertThat(parsed.rows().get(0).weight()).isEqualByComparingTo("0.5");
    assertThat(parsed.rows().get(1).code()).isEqualTo("A-1");
    assertThat(parsed.rows().get(1).name()).isEqualTo("安全操作");
    assertThat(parsed.rows().get(3).importance()).isEqualTo("X");
    assertThat(parsed.rows().get(8).code()).isEqualTo("B-1-1-1");
  }

  @Test
  void parsesCompetitionPracticalThirteenColumnHeaderAndCarriesMergedCells() throws Exception {
    Path file = tempDir.resolve("practical-blueprint.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("实操细目表");
      sheet.createRow(0).createCell(0).setCellValue("氧化铝焙烧工三级实操细目表");
      String[] grouped = {"考评范围一级", "一级名称", "一级权重", "考评范围二级", "二级名称",
          "二级权重", "考评范围", "主题权重", "选考方式", "考评点", "考评点名称", "重要程度", "试题量"};
      var group = sheet.createRow(1);
      for (int i = 0; i < grouped.length; i++) group.createCell(i).setCellValue(grouped[i]);
      var header = sheet.createRow(2);
      for (int i = 0; i < grouped.length; i++) header.createCell(i).setCellValue(grouped[i]);
      writePracticalRow(sheet.createRow(3), "A", "基础", "100%", "A-1", "操作", "100%",
          "安全操作", "100%", "必考", "A-1-1", "佩戴防护用品", "X", "3");
      writePracticalRow(sheet.createRow(4), null, null, null, null, null, null, null, null, null,
          "A-1-2", "检查设备", "Y", "3");
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 0, 0));
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 1, 1));
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 2, 2));
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 3, 3));
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 4, 4));
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 5, 5));
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 6, 6));
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 7, 7));
      sheet.addMergedRegion(new CellRangeAddress(3, 4, 8, 8));
      try (var output = Files.newOutputStream(file)) { workbook.write(output); }
    }

    var parsed = parser.parse(file, "practical");

    assertThat(parsed.rows()).hasSize(5);
    assertThat(parsed.rows()).extracting(CompetitionBlueprintWorkbookParser.ParsedNode::nodeLevel)
        .containsExactly(1, 2, 3, 4, 4);
    assertThat(parsed.rows().get(2).name()).isEqualTo("安全操作");
    assertThat(parsed.rows().get(2).selectMode()).isEqualTo("必考");
    assertThat(parsed.rows().get(3).importance()).isEqualTo("X");
    assertThat(parsed.rows().get(4).questionQuota()).isEqualTo(3);
  }

  @Test
  void parsesStandardCompositePracticalTemplateAsFourLevelHierarchy() throws Exception {
    Path file = tempDir.resolve("standard-practical-blueprint.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("实操细目表");
      sheet.createRow(0).createCell(0).setCellValue("竞赛实操细目表导入模板");
      sheet.createRow(1).createCell(0).setCellValue("职业：氧化铝制取工（氧化铝焙烧工）");

      var group = sheet.createRow(2);
      put(group, 0, "考评范围一级");
      put(group, 4, "考评范围二级");
      put(group, 9, "考评点");
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 4, 8));
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 9, 12));

      String[] headers = {"代码\n（重要程度比例）", "名称", "考评比重", "选考方式",
          "代码\n（重要程度比例）", "名称", "技能要求", "考评比重", "选考方式",
          "代码", "名称", "重要程度", "试题量"};
      var header = sheet.createRow(3);
      for (int i = 0; i < headers.length; i++) put(header, i, headers[i]);

      writeStandardPracticalRow(sheet.createRow(4), "A（48:6:3）", "技能操作", "100%", "必考",
          "A（10:0:0）", "生产准备", "设备准备", "8%", "必考", "001", "检查设备", "X", "3");
      writeStandardPracticalRow(sheet.createRow(5), null, null, null, null, null, null, null, null, null,
          "002", "润滑设备", "X", "3");
      writeStandardPracticalRow(sheet.createRow(6), null, null, null, null, null, null, "流程准备", "4%", "必考",
          "001", "准备流程", "Y", "3");
      try (var output = Files.newOutputStream(file)) { workbook.write(output); }
    }

    var parsed = parser.parse(file, "practical");

    assertThat(parsed.rows()).hasSize(7);
    assertThat(parsed.rows()).extracting(CompetitionBlueprintWorkbookParser.ParsedNode::nodeLevel)
        .containsExactly(1, 2, 3, 4, 4, 3, 4);
    assertThat(parsed.rows().get(0).selectMode()).isEqualTo("必考");
    assertThat(parsed.rows().get(1).weight()).isEqualByComparingTo("0.12");
    assertThat(parsed.rows().get(2).name()).isEqualTo("设备准备");
    assertThat(parsed.rows().get(2).weight()).isEqualByComparingTo("0.08");
    assertThat(parsed.rows().get(5).name()).isEqualTo("流程准备");
    assertThat(parsed.rows().get(5).weight()).isEqualByComparingTo("0.04");
    assertThat(parsed.rows().get(3).code()).isEqualTo("001");
    assertThat(parsed.rows().get(6).code()).isEqualTo("001");
    assertThat(parsed.rows().get(3).parentIndex()).isNotEqualTo(parsed.rows().get(6).parentIndex());
  }

  @Test
  void keepsNumberedSkillRequirementsOnSelectableSecondLevelNodes() throws Exception {
    Path file = tempDir.resolve("skill-requirement-practical-blueprint.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("实操细目表");
      sheet.createRow(0).createCell(0).setCellValue("矿山生产集控员（二级/技师）操作技能考评要素细目表");
      sheet.createRow(1).createCell(0).setCellValue("职业：矿山生产集控员 级别：二级/技师");
      var group = sheet.createRow(2);
      put(group, 0, "考评范围一级"); put(group, 4, "考评范围二级"); put(group, 9, "考评点");
      String[] headers = {"代码\n（重要程度比例）", "名称", "考评比重", "选考方式",
          "代码\n（重要程度比例）", "名称", "技能要求", "考评比重", "选考方式",
          "代码", "名称", "重要程度", "试题量"};
      var header = sheet.createRow(3);
      for (int i = 0; i < headers.length; i++) put(header, i, headers[i]);
      writeStandardPracticalRow(sheet.createRow(4), "A\n（5:1:0）", "作业环境确认", "10%", "必考",
          "A\n（1:1:0）", "设备准备", "1.1.1能启动设备\n1.1.2能调整参数", "10%", "任选一项",
          "001", "启动设备", "X", null);
      writeStandardPracticalRow(sheet.createRow(5), null, null, null, null, null, null, null, null, null,
          "002", "调整参数", "Y", null);
      writeStandardPracticalRow(sheet.createRow(6), null, null, null, null,
          "B\n（4:0:0）", "记录识读与分析", "1.2.1能识读记录", "10%", "任选一项",
          "001", "识读记录", "X", null);
      writeStandardPracticalRow(sheet.createRow(7), "B\n（2:0:0）", "生产监控管理", "90%", "必考",
          "A\n（2:0:0）", "现场监控", "2.1.1能调整监控设备", "90%", "任选一项",
          "001", "调整监控设备", "X", null);
      try (var output = Files.newOutputStream(file)) { workbook.write(output); }
    }

    var parsed = parser.parse(file, "practical");

    assertThat(parsed.rows()).hasSize(9);
    assertThat(parsed.rows()).extracting(CompetitionBlueprintWorkbookParser.ParsedNode::nodeLevel)
        .containsExactly(1, 2, 3, 3, 2, 3, 1, 2, 3);
    assertThat(parsed.rows().get(0).selectMode()).isEqualTo("必考");
    assertThat(parsed.rows().get(1).name()).isEqualTo("设备准备");
    assertThat(parsed.rows().get(1).weight()).isEqualByComparingTo("0.10");
    assertThat(parsed.rows().get(1).selectMode()).isEqualTo("任选一项");
    assertThat(parsed.rows().get(1).skillRequirement()).contains("1.1.1能启动设备");
    assertThat(parsed.rows()).noneMatch(row -> row.name().startsWith("1.1.1"));
  }

  @Test
  void parsesTheoryTemplateAdaptedFromBusinessQuestionBankHierarchy() throws Exception {
    Path file = tempDir.resolve("business-theory-blueprint.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("理论细目表");
      sheet.createRow(0).createCell(0).setCellValue("竞赛理论细目表导入模板");
      sheet.createRow(1).createCell(0).setCellValue("业务题库层级整理");
      String[] headers = {"考评范围一级（行为领域）", "一级权重", "考评范围二级（一级考核范围）",
          "二级权重", "考评范围三级（二级考核范围）", "三级权重", "相关知识", "考评点代码",
          "考评点名称（鉴定点）", "重要程度", "试题量"};
      var header = sheet.createRow(2);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      var row = sheet.createRow(3);
      put(row, 0, "基本要求"); put(row, 1, "50%"); put(row, 2, "职业道德"); put(row, 3, "100%");
      put(row, 4, "职业道德基本知识"); put(row, 5, "100%"); put(row, 6, "安全规范");
      put(row, 7, "A-1-1-1"); put(row, 8, "工匠精神培养"); put(row, 9, "X"); put(row, 10, "10");
      try (var output = Files.newOutputStream(file)) { workbook.write(output); }
    }

    var parsed = parser.parse(file, "theory");

    assertThat(parsed.rows()).extracting(CompetitionBlueprintWorkbookParser.ParsedNode::nodeLevel)
        .containsExactly(1, 2, 3, 4);
    assertThat(parsed.rows().get(0).weight()).isEqualByComparingTo("0.5");
    assertThat(parsed.rows().get(1).name()).isEqualTo("职业道德");
    assertThat(parsed.rows().get(3).name()).isEqualTo("工匠精神培养");
    assertThat(parsed.rows().get(3).questionQuota()).isEqualTo(10);
  }

  private void writeTheoryRow(org.apache.poi.ss.usermodel.Row row, String l1, String l1Weight,
      String l2, String l2Weight, String l3, String l3Weight, String related, String code,
      String name, String importance) {
    put(row, 0, l1); put(row, 1, l1Weight); put(row, 2, l2); put(row, 3, l2Weight);
    put(row, 4, l3); put(row, 5, l3Weight); put(row, 6, related); put(row, 7, code);
    put(row, 8, name); put(row, 9, importance);
  }

  private void writePracticalRow(org.apache.poi.ss.usermodel.Row row, String l1Code, String l1Name,
      String l1Weight, String l2Code, String l2Name, String l2Weight, String topicName,
      String topicWeight, String selectMode, String pointCode, String pointName,
      String importance, String quota) {
    put(row, 0, l1Code); put(row, 1, l1Name); put(row, 2, l1Weight); put(row, 3, l2Code);
    put(row, 4, l2Name); put(row, 5, l2Weight); put(row, 6, topicName); put(row, 7, topicWeight);
    put(row, 8, selectMode); put(row, 9, pointCode); put(row, 10, pointName);
    put(row, 11, importance); put(row, 12, quota);
  }

  private void writeStandardPracticalRow(org.apache.poi.ss.usermodel.Row row, String l1Code,
      String l1Name, String l1Weight, String l1Select, String l2Code, String l2Name,
      String topicName, String topicWeight, String topicSelect, String pointCode,
      String pointName, String importance, String quota) {
    put(row, 0, l1Code); put(row, 1, l1Name); put(row, 2, l1Weight); put(row, 3, l1Select);
    put(row, 4, l2Code); put(row, 5, l2Name); put(row, 6, topicName); put(row, 7, topicWeight);
    put(row, 8, topicSelect); put(row, 9, pointCode); put(row, 10, pointName);
    put(row, 11, importance); put(row, 12, quota);
  }

  private void put(org.apache.poi.ss.usermodel.Row row, int column, String value) {
    if (value != null) row.createCell(column).setCellValue(value);
  }
}
