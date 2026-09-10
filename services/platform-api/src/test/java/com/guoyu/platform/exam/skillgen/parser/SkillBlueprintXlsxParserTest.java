package com.guoyu.platform.exam.skillgen.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintParseResult;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintParseResult.NodeRow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * 操作技能细目表 xlsx 解析器单测（方案 §2.5 画像 / IMP-1~IMP-5）。
 *
 * <p>画像：1 sheet、R3 块表头（A3:D3 一级 / E3:I3 二级 / J3:M3 考评点）、R4 子表头、数据 R5 起、
 * 页脚"填表人"截断；A/E 列为「代码⏎（X:Y:Z）」；G 列技能要求多行带多余空格；M 列试题量（可空）。
 */
class SkillBlueprintXlsxParserTest {

  private static final String[] SUB_HEADER = {
      "代码\n（重要程度比例）", "名称", "考评比重", "选考方式",
      "代码\n（重要程度比例）", "名称", "技能要求", "考评比重", "选考方式",
      "代码", "名称", "重要程度", "试题量"
  };

  /** 按样例结构构造：2 一级 / 3 二级 / 5 考评点；A/E 列合并（值下填充） */
  private ByteArrayInputStream buildSampleXlsx(boolean withFooter, String quotaValue) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet s = wb.createSheet("Sheet1");
      s.createRow(0).createCell(0)
          .setCellValue("矿山生产集控员（二级/技师）操作技能考评要素细目表");
      s.createRow(1).createCell(0).setCellValue("职业： 矿山生产集控员                级别： 二级/技师");
      Row blockHeader = s.createRow(2);
      blockHeader.createCell(0).setCellValue("考评范围一级");
      blockHeader.createCell(4).setCellValue("考评范围二级");
      blockHeader.createCell(9).setCellValue("考评点");
      Row sub = s.createRow(3);
      for (int c = 0; c < SUB_HEADER.length; c++) {
        sub.createCell(c).setCellValue(SUB_HEADER[c]);
      }
      // 数据行（第 5 行起）：A 作业环境确认（2 二级，2 点）；B 生产监控管理（1 二级，3 点）
      // A=一级代码⏎(X:Y:Z) B=一级名称 C=一级权重 D=一级选考 E=二级代码⏎(X:Y:Z) F=二级名称
      // G=技能要求 H=二级权重 I=二级选考 J=点码 K=点名 L=重要度 M=试题量
      String[][] rows = {
          {"A\n（2:0:0）", "作业环境确认", "0.3", "必考", "A\n（1:0:0）", "设备准备",
              "1.1.1 能对 集控室 监控设备进行联机启动\n1.1.2 能对集控室监控设备基本参数进行调整",
              "0.3", "任选一项", "001", "联机启动集控室监控设备", "X", quotaValue},
          {"", "", "", "", "", "", "", "", "", "002", "调整集控室监控设备基本参数", "X", ""},
          {"", "", "", "", "B\n（1:0:0）", "记录识读与分析",
              "1.2.1 能对特殊生产数据记录进行识读", "0.6", "任选一项", "001", "识读特殊生产数据记录", "Y", ""},
          {"B\n（3:0:0）", "生产监控管理", "0.7", "必考", "A\n（3:0:0）", "生产现场监控",
              "2.1.1 能对监控设备联机方案进行调整", "0.7", "任选一项", "001", "调整监控设备联机方案", "X", ""},
          {"", "", "", "", "", "", "", "", "", "002", "进行监控设备联机调整运行", "X", ""},
          {"", "", "", "", "", "", "", "", "", "003", "监控设备故障状况分析", "Z", ""},
      };
      for (int i = 0; i < rows.length; i++) {
        Row r = s.createRow(4 + i);
        for (int c = 0; c < rows[i].length; c++) {
          r.createCell(c).setCellValue(rows[i][c]);
        }
      }
      // 合并：A 4-7（一级A）、8-9（一级B）；E 4-5、6-7、8-10
      // 数据行布局：POI 4-6=一级A（2 个二级：4-5 设备准备 / 6 记录识读），7-9=一级B（二级 7-9 生产现场监控）
      s.addMergedRegion(new CellRangeAddress(4, 6, 0, 0));
      s.addMergedRegion(new CellRangeAddress(7, 9, 0, 0));
      s.addMergedRegion(new CellRangeAddress(4, 5, 4, 4));
      s.addMergedRegion(new CellRangeAddress(7, 9, 4, 4));
      if (withFooter) {
        Row footer = s.createRow(10);
        footer.createCell(0).setCellValue("填表人：");
        footer.createCell(2).setCellValue("填表日期：20  年  月  日");
      }
      wb.write(out);
      return new ByteArrayInputStream(out.toByteArray());
    }
  }

  @Test
  void imp1_sampleStructureParsed() throws Exception {
    SkillBlueprintParseResult result = new SkillBlueprintXlsxParser()
        .parse(buildSampleXlsx(true, ""), "样例.xlsx");
    List<NodeRow> rows = result.getRows();
    long l1 = rows.stream().filter(n -> n.getNodeLevel() == 1).count();
    long l2 = rows.stream().filter(n -> n.getNodeLevel() == 2).count();
    long points = rows.stream().filter(n -> n.getNodeLevel() == 3).count();
    assertEquals(2, l1, "一级块数");
    assertEquals(3, l2, "二级块数");
    assertEquals(6, points, "考评点数");
    // 权重/选考方式/XYZ
    NodeRow first = rows.get(0);
    assertEquals("A", first.getCode());
    assertEquals("作业环境确认", first.getName());
    assertEquals("2:0:0", first.getXyzStat());
    assertEquals("必考", first.getSelectMode());
    assertEquals(0, first.getWeight().compareTo(new java.math.BigDecimal("30.00")));
    // full_code 由应用服务按父链拼 A-A-001；此处核验考评点级别关系
    NodeRow point = rows.stream().filter(n -> n.getNodeLevel() == 3).findFirst().orElseThrow();
    assertEquals("001", point.getCode());
    assertEquals("X", point.getImportance());
  }

  @Test
  void imp1b_xyzRatio18to5Style() throws Exception {
    SkillBlueprintParseResult result = new SkillBlueprintXlsxParser()
        .parse(buildSampleXlsx(false, ""), "样例.xlsx");
    long x = result.getRows().stream()
        .filter(n -> n.getNodeLevel() == 3 && "X".equals(n.getImportance())).count();
    long y = result.getRows().stream()
        .filter(n -> n.getNodeLevel() == 3 && "Y".equals(n.getImportance())).count();
    long z = result.getRows().stream()
        .filter(n -> n.getNodeLevel() == 3 && "Z".equals(n.getImportance())).count();
    assertEquals(4, x);
    assertEquals(1, y);
    assertEquals(1, z);
  }

  @Test
  void imp2_mergedFillDownAndSkillRequirementNormalized() {
    try {
      SkillBlueprintParseResult result = new SkillBlueprintXlsxParser()
          .parse(buildSampleXlsx(false, ""), "样例.xlsx");
      // 合并下填充：第 6 行（"002" 续行）仍归属二级 A 设备准备（其技能要求非空）
      NodeRow l2First = result.getRows().stream()
          .filter(n -> n.getNodeLevel() == 2).findFirst().orElseThrow();
      assertNotNull(l2First.getSkillRequirement());
      // 空白规范化："能对 集控室 监控设备…" → 去掉中文间空格
      assertTrue(l2First.getSkillRequirement().contains("能对集控室监控设备进行联机启动"),
          "实际: " + l2First.getSkillRequirement());
      assertTrue(!l2First.getSkillRequirement().contains("集控室 监控"));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Test
  void imp3_quotaColumnHandled() throws Exception {
    // 空 → null
    SkillBlueprintParseResult empty = new SkillBlueprintXlsxParser()
        .parse(buildSampleXlsx(false, ""), "样例.xlsx");
    assertNull(empty.getRows().stream().filter(n -> n.getNodeLevel() == 3).findFirst()
        .orElseThrow().getQuestionQuota());
    // "5" → 5
    SkillBlueprintParseResult five = new SkillBlueprintXlsxParser()
        .parse(buildSampleXlsx(false, "5"), "样例.xlsx");
    assertEquals(5, five.getRows().stream().filter(n -> n.getNodeLevel() == 3).findFirst()
        .orElseThrow().getQuestionQuota());
    // "abc" → 记警告不抛异常
    SkillBlueprintParseResult bad = new SkillBlueprintXlsxParser()
        .parse(buildSampleXlsx(false, "abc"), "样例.xlsx");
    assertTrue(bad.getWarnings().stream().anyMatch(w -> w.contains("试题量列异常")));
  }

  @Test
  void imp5_footerTruncated() throws Exception {
    SkillBlueprintParseResult result = new SkillBlueprintXlsxParser()
        .parse(buildSampleXlsx(true, ""), "样例.xlsx");
    assertTrue(result.getRows().stream().noneMatch(n -> n.getName() != null
        && n.getName().contains("填表人")));
  }

  @Test
  void imp6_badFileRejected() {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet s = wb.createSheet("Sheet1");
      s.createRow(0).createCell(0).setCellValue("没有表头的文件");
      wb.write(out);
      ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
      var ex = org.junit.jupiter.api.Assertions.assertThrows(
          com.guoyu.platform.common.exception.BusinessException.class,
          () -> new SkillBlueprintXlsxParser().parse(in, "bad.xlsx"));
      assertTrue(ex.getMessage().contains("考评范围一级"));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
