package com.guoyu.platform.exam.theorygen.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.theorygen.dto.BlueprintParseResult;
import com.guoyu.platform.exam.theorygen.dto.BlueprintParseResult.NodeRow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * 细目表 xlsx 解析器单测。
 *
 * <p>基准画像对照生产样例《氧化铝制取工(五级-初级工)轻冶料浆配料工》：
 * 数据自第 5 行起；A 列跨行合并（一级 2 块）；单元格多行为「名称⏎代码⏎（X:Y:Z）」；
 * 权重为小数（0.45=45%）。
 */
class TheoryBlueprintXlsxParserTest {

  /** 构造一个 2 一级 / 3 二级 / 4 三级 / 8 考评点的小样例（结构同生产样例） */
  private ByteArrayInputStream buildSampleXlsx() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet s = wb.createSheet("Sheet1");
      // 表头（行 1~4，POI 0 基）
      s.createRow(0).createCell(0).setCellValue("样例细目表");
      // 数据行从 rowIdx=4 开始（对应 Excel 第 5 行）
      // 一级 A 基本要求（rows 4-7），一级 B 工作要求（rows 8-11）
      String[][] rows = {
          // A=一级, B=权重, C=二级, D=权重, E=三级, F=权重, G=相关知识, H=点码, I=点名, J=重要度
          {"基本要求\nA\n（5:1:1）", "0.45", "职业道德\nA\n（2:0:1）", "0.05",
              "职业道德基本知识\nA\n（2:0:0）", "0.01", "职业道道德基本内涵", "001", "职业道德基本内涵", "X"},
          {"", "", "", "", "", "", "", "002", "职业守则与规范", "X"},
          {"", "", "基础知识\nB\n（3:1:0）", "0.4", "基础理论知识\nA\n（3:1:0）", "0.15",
              "1.氧化铝生产原理。", "001", "氧化铝的定义与性质", "X"},
          {"", "", "", "", "", "", "", "002", "氧化铝生产方法分类", "Y"},
          {"工作要求\nB\n（6:1:0）", "0.55", "生产准备\nA\n（3:0:0）", "0.1",
              "配料准备\nA\n（3:0:0）", "0.05", "1.配料计算基本方法。", "001", "配料计算基本方法", "X"},
          {"", "", "", "", "", "", "", "002", "料浆配料操作", "X"},
          {"", "", "配料操作\nB\n（3:1:0）", "0.45", "料浆调配\nA\n（3:1:0）", "0.2",
              "1.料浆配料操作规程。", "001", "料浆成分调整方法", "X"},
          {"", "", "", "", "", "", "", "002", "配料设备维护", "Z"},
      };
      for (int i = 0; i < rows.length; i++) {
        Row r = s.createRow(4 + i);
        for (int c = 0; c < rows[i].length; c++) {
          r.createCell(c).setCellValue(rows[i][c]);
        }
      }
      // 合并：A 列 4-7、8-11；B 列同；C/D：4-5（职业道德）、6-7（基础知识）、8-9（生产准备）、10-11（配料操作）
      s.addMergedRegion(new CellRangeAddress(4, 7, 0, 0));
      s.addMergedRegion(new CellRangeAddress(4, 7, 1, 1));
      s.addMergedRegion(new CellRangeAddress(8, 11, 0, 0));
      s.addMergedRegion(new CellRangeAddress(8, 11, 1, 1));
      s.addMergedRegion(new CellRangeAddress(4, 5, 2, 2));
      s.addMergedRegion(new CellRangeAddress(4, 5, 3, 3));
      s.addMergedRegion(new CellRangeAddress(6, 7, 2, 2));
      s.addMergedRegion(new CellRangeAddress(6, 7, 3, 3));
      s.addMergedRegion(new CellRangeAddress(8, 9, 2, 2));
      s.addMergedRegion(new CellRangeAddress(8, 9, 3, 3));
      s.addMergedRegion(new CellRangeAddress(10, 11, 2, 2));
      s.addMergedRegion(new CellRangeAddress(10, 11, 3, 3));
      wb.write(out);
      return new ByteArrayInputStream(out.toByteArray());
    }
  }

  @Test
  void parsesSampleStructure() throws Exception {
    TheoryBlueprintXlsxParser parser = new TheoryBlueprintXlsxParser();
    BlueprintParseResult result = parser.parse(buildSampleXlsx(), "sample.xlsx");
    List<NodeRow> rows = result.getRows();
    // 2 一级 + 4 二级 + 4 三级 + 8 考评点 = 18
    assertEquals(18, rows.size());
    assertEquals(8, rows.stream().filter(r -> r.getNodeLevel() == 4).count());
    // 一级解析：名称+代码+比例
    NodeRow l1 = rows.get(0);
    assertEquals(1, l1.getNodeLevel());
    assertEquals("A", l1.getCode());
    assertEquals("基本要求", l1.getName());
    assertEquals("5:1:1", l1.getXyzStat());
    assertEquals(0, l1.getWeight().compareTo(new java.math.BigDecimal("45.00")), "0.45 → 45.00");
    // 合并下填充：第 2 行考评点仍挂在原三级
    NodeRow point2 = rows.stream().filter(r -> r.getNodeLevel() == 4 && "002".equals(r.getCode())
        && "职业守则与规范".equals(r.getName())).findFirst().orElseThrow();
    assertNotNull(point2.getParentIndex());
    assertEquals(1, point2.getParentIndex() > 0 ? 1 : 0, "父索引应指向三级节点行");
    // 考评点重要程度
    NodeRow pointY = rows.stream().filter(r -> r.getNodeLevel() == 4
        && "Y".equals(r.getImportance())).findFirst().orElse(null);
    assertNotNull(pointY, "应解析到 Y 重要程度考评点");
    NodeRow pointZ = rows.stream().filter(r -> r.getNodeLevel() == 4
        && "Z".equals(r.getImportance())).findFirst().orElse(null);
    assertNotNull(pointZ);
    // 三级相关知识归一（换行 → 分号）
    NodeRow l3 = rows.stream().filter(r -> r.getNodeLevel() == 3
        && "配料准备".equals(r.getName())).findFirst().orElseThrow();
    assertEquals("1.配料计算基本方法。", l3.getRelatedKnowledge());
    // full_code 由 service 层拼装，这里 parentIndex 链正确即可：每个考评点父为 L3
    for (NodeRow r : rows) {
      if (r.getNodeLevel() == 4) {
        assertEquals(3, rows.get(r.getParentIndex()).getNodeLevel(), "考评点父应为三级节点");
      }
    }
  }

  @Test
  void parseWeightVariants() {
    assertEquals(0, BlueprintCellTextParser.parseWeight("0.45")
        .compareTo(new java.math.BigDecimal("45.00")));
    assertEquals(0, BlueprintCellTextParser.parseWeight("45%")
        .compareTo(new java.math.BigDecimal("45.00")));
    assertEquals(0, BlueprintCellTextParser.parseWeight("100")
        .compareTo(new java.math.BigDecimal("100.00")));
    assertNull(BlueprintCellTextParser.parseWeight(""));
    assertNull(BlueprintCellTextParser.parseWeight(null));
  }

  @Test
  void cellTextParserHandlesBothOrders() {
    // 样例顺序：名称⏎代码⏎（X:Y:Z）
    BlueprintCellTextParser.CellParts p1 = BlueprintCellTextParser.parse("基本要求\nA\n（80:7:3）");
    assertEquals("基本要求", p1.name());
    assertEquals("A", p1.code());
    assertEquals("80:7:3", p1.xyzStat());
    // 指引模板顺序：代码⏎名称⏎（X:Y:Z）
    BlueprintCellTextParser.CellParts p2 = BlueprintCellTextParser.parse("A\n基本要求\n（80:7:3）");
    assertEquals("基本要求", p2.name());
    assertEquals("A", p2.code());
    assertEquals("80:7:3", p2.xyzStat());
    // 无比例
    BlueprintCellTextParser.CellParts p3 = BlueprintCellTextParser.parse("基础知识\nB");
    assertEquals("基础知识", p3.name());
    assertEquals("B", p3.code());
    assertNull(p3.xyzStat());
  }

  @Test
  void emptySheetYieldsEmptyRows() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      wb.createSheet("Sheet1");
      wb.write(out);
      TheoryBlueprintXlsxParser parser = new TheoryBlueprintXlsxParser();
      BlueprintParseResult result = parser.parse(new ByteArrayInputStream(out.toByteArray()), "e.xlsx");
      assertTrue(result.getRows().isEmpty());
    }
  }
}
