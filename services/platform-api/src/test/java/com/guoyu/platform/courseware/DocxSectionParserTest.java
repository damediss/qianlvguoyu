package com.guoyu.platform.courseware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import com.guoyu.platform.courseware.service.DocxSectionParser;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

/** U-01~U-03:docx 小节切分(标题样式/编号兜底/表格管道化/空文档) */
class DocxSectionParserTest {

  private final DocxSectionParser parser = new DocxSectionParser();

  /** 构造带标题样式的 docx(模拟规范标准文档) */
  private static byte[] buildDocxWithStyles() throws Exception {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      XWPFStyles styles = doc.createStyles();
      addStyle(styles, "H1", "Heading 1");
      addStyle(styles, "H2", "标题 2");

      XWPFParagraph h1 = doc.createParagraph();
      h1.setStyle("H1");
      h1.createRun().setText("第一章 职业概况");
      doc.createParagraph().createRun().setText("本章介绍职业的基本情况。");

      XWPFParagraph h2 = doc.createParagraph();
      h2.setStyle("H2");
      h2.createRun().setText("第一节 职业定义");
      doc.createParagraph().createRun().setText("使用特定设备进行材料热处理。");

      XWPFTable table = doc.createTable(2, 2);
      XWPFTableRow row0 = table.getRow(0);
      setCell(row0.getCell(0), "项目");
      setCell(row0.getCell(1), "内容");
      XWPFTableRow row1 = table.getRow(1);
      setCell(row1.getCell(0), "温度");
      setCell(row1.getCell(1), "850℃");
      doc.createParagraph().createRun().setText("表格后正文。");

      doc.write(out);
      return out.toByteArray();
    }
  }

  private static void addStyle(XWPFStyles styles, String styleId, String name) {
    CTStyle ctStyle = CTStyle.Factory.newInstance();
    ctStyle.setStyleId(styleId);
    ctStyle.setType(STStyleType.PARAGRAPH);
    ctStyle.addNewName().setVal(name);
    XWPFStyle style = new XWPFStyle(ctStyle, styles);
    styles.addStyle(style);
  }

  private static void setCell(XWPFTableCell cell, String text) {
    cell.removeParagraph(0);
    cell.addParagraph().createRun().setText(text);
  }

  /** 构造无样式、只有编号文本的 docx(兜底路径) */
  private static byte[] buildDocxWithNumbering() throws Exception {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      doc.createParagraph().createRun().setText("第一部分 基本要求");
      doc.createParagraph().createRun().setText("本部分给出总体要求。");
      doc.createParagraph().createRun().setText("1.1 职业道德");
      doc.createParagraph().createRun().setText("遵守法律法规。");
      doc.createParagraph().createRun().setText("一、基本知识");
      doc.createParagraph().createRun().setText("热处理基础。");
      doc.write(out);
      return out.toByteArray();
    }
  }

  @Test
  void u01_headingStyleSplitsSections() {
    List<SourceSection> sections = parser.parse(buildDocxWithStylesUnchecked());
    assertEquals(2, sections.size());
    assertEquals("第一章 职业概况", sections.get(0).heading());
    assertEquals(1, sections.get(0).level());
    assertEquals("第一节 职业定义", sections.get(1).heading());
    assertEquals(2, sections.get(1).level());
    assertTrue(sections.get(1).text().contains("使用特定设备"));
  }

  @Test
  void u02_numberingFallbackSplitsSections() {
    List<SourceSection> sections = parser.parse(buildDocxUnchecked());
    assertEquals(3, sections.size());
    assertEquals("第一部分 基本要求", sections.get(0).heading());
    assertEquals(1, sections.get(0).level());
    assertEquals("1.1 职业道德", sections.get(1).heading());
    assertEquals(2, sections.get(1).level());
    assertEquals("一、基本知识", sections.get(2).heading());
    assertEquals(2, sections.get(2).level());
  }

  @Test
  void u03_tablePipedIntoSection() {
    List<SourceSection> sections = parser.parse(buildDocxWithStylesUnchecked());
    // 表格并入"第一节 职业定义"小节,单元格管道化
    String text = sections.get(1).text();
    assertTrue(text.contains("项目 | 内容"), "表格应管道化并入小节: " + text);
    assertTrue(text.contains("温度 | 850℃"));
    assertTrue(text.contains("表格后正文"));
  }

  @Test
  void u03_emptyDocumentReturnsEmpty() throws Exception {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      doc.createParagraph().createRun().setText("");
      doc.write(out);
      assertTrue(parser.parse(out.toByteArray()).isEmpty());
    }
  }

  // ── 包装:checked exception 处理(测试可读性) ──
  private static byte[] buildDocxWithStylesUnchecked() {
    try {
      return buildDocxWithStyles();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] buildDocxUnchecked() {
    try {
      return buildDocxWithNumbering();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
