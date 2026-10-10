package com.guoyu.platform.courseware.service;

import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareLayout.Rect;
import com.guoyu.platform.courseware.domain.CoursewareLayout.SlotSpec;
import com.guoyu.platform.courseware.domain.CoursewareLayout.TextStyle;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideBlock;
import com.guoyu.platform.courseware.domain.CoursewareTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.TextParagraph.TextAlign;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextFont;

/**
 * PPTX 原生渲染器(方案 §10):内容块 → POI XSLF 原生对象(文本框/表格/KPI 组合框),
 * 空白版式 + 槽位几何全由模板数据决定;中文字体 latin/ea/cs 三写;页脚来源标注;导出后回读验证。
 */
public final class PptxRenderer {

  private PptxRenderer() {
  }

  public static final String PPTX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.presentationml.presentation";

  /** 一页的渲染输入 */
  public record RenderSlide(String layoutId, List<SlideBlock> blocks, String speakerNotes,
                            String footerText, String pageRole) {
  }

  /** 渲染整份课件为 PPTX 字节 */
  public static byte[] render(List<RenderSlide> slides) {
    try (XMLSlideShow ppt = new XMLSlideShow();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ppt.setPageSize(new Dimension((int) CoursewareTheme.CANVAS_W_PT,
          (int) CoursewareTheme.CANVAS_H_PT));
      XSLFSlideMaster master = ppt.getSlideMasters().get(0);
      XSLFSlideLayout blank = master.getLayout(SlideLayout.BLANK);

      for (RenderSlide renderSlide : slides) {
        CoursewareLayout.Layout layout = CoursewareLayout.require(renderSlide.layoutId());
        XSLFSlide slide = ppt.createSlide(blank);
        for (SlideBlock block : renderSlide.blocks()) {
          SlotSpec slot = layout.slot(block.slotId());
          if (slot == null) {
            continue; // 结构校验已拦截;渲染侧防御性跳过
          }
          renderBlock(slide, block, slot);
        }
        renderFooter(slide, renderSlide.footerText(), renderSlide.pageRole(), renderSlide.layoutId());
        writeNotes(slide, renderSlide.speakerNotes());
      }
      ppt.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("PPTX 渲染失败: " + e.getMessage(), e);
    }
  }

  // ──────────────── 块渲染 ────────────────

  private static void renderBlock(XSLFSlide slide, SlideBlock block, SlotSpec slot) {
    switch (block.type()) {
      case SlideBlock.TYPE_TEXT -> renderText(slide, block, slot);
      case SlideBlock.TYPE_BULLETS -> renderBullets(slide, block, slot);
      case SlideBlock.TYPE_TABLE -> renderTable(slide, block, slot);
      case SlideBlock.TYPE_KPI -> renderKpi(slide, block, slot);
      default -> {
        // 未知类型不渲染(结构校验已记录)
      }
    }
  }

  private static void renderText(XSLFSlide slide, SlideBlock block, SlotSpec slot) {
    String text = block.text() == null ? "" : block.text();
    XSLFTextBox box = createBox(slide, slot.rect());
    XSLFTextParagraph p = firstParagraph(box);
    applyParagraphAlign(p, slot.style());
    addRun(p, text, slot.style());
  }

  private static void renderBullets(XSLFSlide slide, SlideBlock block, SlotSpec slot) {
    List<String> items = block.items() == null ? List.of() : block.items();
    XSLFTextBox box = createBox(slide, slot.rect());
    boolean first = true;
    for (String item : items) {
      XSLFTextParagraph p = first ? firstParagraph(box) : box.addNewTextParagraph();
      first = false;
      applyParagraphAlign(p, slot.style());
      p.setBullet(true);
      p.setSpaceAfter(CoursewareTheme.BULLET_GAP_PT);
      addRun(p, item == null ? "" : item, slot.style());
    }
    if (items.isEmpty()) {
      addRun(firstParagraph(box), "", slot.style());
    }
  }

  private static void renderTable(XSLFSlide slide, SlideBlock block, SlotSpec slot) {
    List<String> header = block.header() == null ? List.of() : block.header();
    List<List<String>> rows = block.rows() == null ? List.of() : block.rows();
    int cols = Math.max(1, header.size());
    int rowCount = rows.size() + 1;
    Rect rect = slot.rect();
    XSLFTable table = slide.createTable(rowCount, cols);
    table.setAnchor(new Rectangle2D.Double(rect.x(), rect.y(), rect.w(), rect.h()));
    double colW = rect.w() / cols;
    for (int c = 0; c < cols; c++) {
      table.setColumnWidth(c, colW);
    }
    table.setRowHeight(0, 30);
    for (int r = 0; r < rowCount; r++) {
      if (r > 0) {
        table.setRowHeight(r, 24);
      }
      for (int c = 0; c < cols; c++) {
        XSLFTableCell cell = table.getCell(r, c);
        String text = r == 0
            ? (c < header.size() ? header.get(c) : "")
            : (c < rows.get(r - 1).size() ? rows.get(r - 1).get(c) : "");
        cell.setText(text == null ? "" : text);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (r == 0) {
          cell.setFillColor(colorOf(CoursewareTheme.PRIMARY));
          styleCell(cell, slot.style().fontSizePt(), true, "FFFFFF");
        } else {
          cell.setFillColor(r % 2 == 0 ? colorOf(CoursewareTheme.SECONDARY) : Color.WHITE);
          styleCell(cell, slot.style().fontSizePt(), false, CoursewareTheme.TEXT);
        }
      }
    }
  }

  /** KPI:槽位矩形内 数值(28pt 主色)+ 标签(15pt)+ 备注(11pt 灰)三段,浅蓝矩形底 */
  private static void renderKpi(XSLFSlide slide, SlideBlock block, SlotSpec slot) {
    Rect rect = slot.rect();
    XSLFAutoShape bg = slide.createAutoShape();
    bg.setAnchor(new Rectangle2D.Double(rect.x(), rect.y(), rect.w(), rect.h()));
    bg.setFillColor(colorOf(CoursewareTheme.SECONDARY));
    bg.setLineColor(colorOf(CoursewareTheme.PRIMARY));

    XSLFTextBox valueBox = createBox(slide,
        new Rect(rect.x(), rect.y() + 24, rect.w(), 52));
    XSLFTextParagraph vp = firstParagraph(valueBox);
    vp.setTextAlign(TextAlign.CENTER);
    addRun(vp, block.value() == null ? "" : block.value(),
        new TextStyle(28, true, CoursewareTheme.PRIMARY, "center"));

    XSLFTextBox labelBox = createBox(slide,
        new Rect(rect.x(), rect.y() + 82, rect.w(), 30));
    XSLFTextParagraph lp = firstParagraph(labelBox);
    lp.setTextAlign(TextAlign.CENTER);
    addRun(lp, block.label() == null ? "" : block.label(),
        new TextStyle(15, false, CoursewareTheme.TEXT, "center"));

    if (block.note() != null && !block.note().isBlank()) {
      XSLFTextBox noteBox = createBox(slide,
          new Rect(rect.x() + 8, rect.y() + 118, rect.w() - 16, 52));
      XSLFTextParagraph np = firstParagraph(noteBox);
      np.setTextAlign(TextAlign.CENTER);
      addRun(np, block.note(), new TextStyle(11, false, "777777", "center"));
    }
  }

  /** 页脚来源标注(方案 §10.9):cover/toc 不标;footerText 为空不渲染 */
  private static void renderFooter(XSLFSlide slide, String footerText, String pageRole,
                                   String layoutId) {
    if (footerText == null || footerText.isBlank()) {
      return;
    }
    if (CoursewareLayout.COVER.equals(layoutId) || CoursewareLayout.TOC.equals(layoutId)) {
      return;
    }
    XSLFTextBox box = createBox(slide, new Rect(CoursewareTheme.FOOTER_X, CoursewareTheme.FOOTER_Y,
        CoursewareTheme.FOOTER_W, CoursewareTheme.FOOTER_H));
    XSLFTextParagraph p = firstParagraph(box);
    addRun(p, footerText,
        new TextStyle(CoursewareTheme.FOOTER_FONT_SIZE, false, CoursewareTheme.FOOTER_GRAY,
            "left"));
  }

  /** 备注页讲稿(方案 §10.4):优先写 notes 正文占位符,兜底新建文本框 */
  static void writeNotes(XSLFSlide slide, String notesText) {
    if (notesText == null || notesText.isBlank()) {
      return;
    }
    // getNotesSlide 为创建语义(slide.getNotes() 在 notes 不存在时返回 null)
    XSLFNotes notes = slide.getSlideShow().getNotesSlide(slide);
    boolean written = false;
    for (XSLFTextShape shape : notes.getPlaceholders()) {
      if (shape.getTextType() == Placeholder.BODY) {
        shape.setText(notesText);
        for (XSLFTextParagraph p : shape.getTextParagraphs()) {
          for (XSLFTextRun r : p.getTextRuns()) {
            setFonts(r, CoursewareTheme.FONT_FAMILY, 12, false, CoursewareTheme.TEXT);
          }
        }
        written = true;
        break;
      }
    }
    if (!written) {
      XSLFTextBox box = notes.createTextBox();
      box.setAnchor(new Rectangle2D.Double(40, 120, 600, 200));
      addRun(firstParagraph(box), notesText,
          new TextStyle(12, false, CoursewareTheme.TEXT, "left"));
    }
  }

  // ──────────────── 回读验证(方案 §10.6) ────────────────

  /**
   * 回读验证:页数一致、每页至少 1 个非空文本形状、首处文本带 ea 中文字体。失败抛 IllegalStateException。
   */
  public static void verify(byte[] bytes, int expectedSlides) {
    try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
      List<XSLFSlide> slides = ppt.getSlides();
      if (slides.size() != expectedSlides) {
        throw new IllegalStateException("回读页数不一致: 期望 " + expectedSlides + ",实际 " + slides.size());
      }
      boolean eaFontSeen = false;
      for (int i = 0; i < slides.size(); i++) {
        boolean hasText = false;
        for (XSLFShape shape : slides.get(i).getShapes()) {
          if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text != null && !text.isBlank()) {
              hasText = true;
            }
            for (XSLFTextParagraph p : textShape.getTextParagraphs()) {
              for (XSLFTextRun r : p.getTextRuns()) {
                if (!eaFontSeen && hasEastAsianFont(r)) {
                  eaFontSeen = true;
                }
              }
            }
          }
          if (shape instanceof XSLFTable table) {
            for (int r = 0; r < table.getNumberOfRows(); r++) {
              for (int c = 0; c < table.getNumberOfColumns(); c++) {
                XSLFTableCell cell = table.getCell(r, c);
                if (cell != null && cell.getText() != null && !cell.getText().isBlank()) {
                  hasText = true;
                }
              }
            }
          }
        }
        if (!hasText) {
          throw new IllegalStateException("第 " + (i + 1) + " 页无非空文本");
        }
      }
      if (!eaFontSeen) {
        throw new IllegalStateException("未检出中文字体(ea typeface)设置");
      }
    } catch (IOException e) {
      throw new IllegalStateException("PPTX 回读失败: " + e.getMessage(), e);
    }
  }

  /** 检查 run 是否带 ea 中文字体设置 */
  static boolean hasEastAsianFont(XSLFTextRun run) {
    if (!(run.getXmlObject() instanceof CTRegularTextRun xmlRun)) {
      return false;
    }
    CTTextCharacterProperties rPr = xmlRun.getRPr();
    if (rPr == null || !rPr.isSetEa()) {
      return false;
    }
    String typeface = rPr.getEa().getTypeface();
    return CoursewareTheme.FONT_FAMILY.equals(typeface);
  }

  // ──────────────── 基础辅助 ────────────────

  private static XSLFTextBox createBox(XSLFSlide slide, Rect rect) {
    XSLFTextBox box = slide.createTextBox();
    box.setAnchor(new Rectangle2D.Double(rect.x(), rect.y(), rect.w(), rect.h()));
    box.setVerticalAlignment(VerticalAlignment.TOP);
    return box;
  }

  /** 文本框首个段落(createTextBox 自带一个空段落,复用避免首行空白) */
  private static XSLFTextParagraph firstParagraph(XSLFTextBox box) {
    List<XSLFTextParagraph> paragraphs = box.getTextParagraphs();
    if (paragraphs.isEmpty()) {
      return box.addNewTextParagraph();
    }
    return paragraphs.get(0);
  }

  private static void applyParagraphAlign(XSLFTextParagraph p, TextStyle style) {
    switch (style.align()) {
      case "center" -> p.setTextAlign(TextAlign.CENTER);
      case "right" -> p.setTextAlign(TextAlign.RIGHT);
      default -> p.setTextAlign(TextAlign.LEFT);
    }
  }

  /** 添加文本 run 并设置字体(中文必须 latin/ea/cs 三写,方案 §10.3) */
  static void addRun(XSLFTextParagraph p, String text, TextStyle style) {
    XSLFTextRun run = p.addNewTextRun();
    run.setText(text);
    setFonts(run, CoursewareTheme.FONT_FAMILY, style.fontSizePt(), style.bold(),
        style.colorHex());
  }

  static void setFonts(XSLFTextRun run, String family, int sizePt, boolean bold, String colorHex) {
    run.setFontFamily(family);
    run.setFontSize((double) sizePt);
    run.setBold(bold);
    run.setFontColor(colorOf(colorHex));
    // ea/cs 东亚字体:setFontFamily 只写 latin,中文必须另写 ea(坑:否则回落默认字体)
    if (run.getXmlObject() instanceof CTRegularTextRun xmlRun) {
      CTTextCharacterProperties rPr = xmlRun.getRPr();
      if (rPr == null) {
        rPr = xmlRun.addNewRPr();
      }
      CTTextFont ea = rPr.isSetEa() ? rPr.getEa() : rPr.addNewEa();
      ea.setTypeface(family);
      CTTextFont cs = rPr.isSetCs() ? rPr.getCs() : rPr.addNewCs();
      cs.setTypeface(family);
    }
  }

  private static void styleCell(XSLFTableCell cell, int fontSizePt, boolean bold, String colorHex) {
    for (XSLFTextParagraph p : cell.getTextParagraphs()) {
      p.setTextAlign(TextAlign.CENTER);
      for (XSLFTextRun r : p.getTextRuns()) {
        setFonts(r, CoursewareTheme.FONT_FAMILY, fontSizePt, bold, colorHex);
      }
    }
  }

  private static Color colorOf(String hex) {
    return new Color(Integer.parseInt(hex, 16));
  }
}
