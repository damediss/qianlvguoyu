package com.guoyu.platform.courseware.service;

import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

/**
 * 标准 docx 小节切分(方案 §6):Heading 样式优先,编号正则兜底(标准 docx 标题样式常不规范),
 * 表格管道化为文本并入当前小节。
 */
@Component
public class DocxSectionParser {

  private static final Pattern HEADING_STYLE = Pattern.compile("^(?:Heading|标题)\\s*(\\d+)$",
      Pattern.CASE_INSENSITIVE);
  /** 兜底:第X章/第X部分/第X篇 → 1 级 */
  private static final Pattern CN_CHAPTER = Pattern.compile("^第[一二三四五六七八九十百千万0-9]+[章部篇]");
  /** 兜底:数字编号 1. / 1.1 / 1.1.1(级数=点号数+1,上限 6) */
  private static final Pattern NUM_SECTION = Pattern.compile("^(\\d+(?:\\.\\d+)*)[、.．\\s]");
  /** 兜底:中文序号 一、二、 → 2 级 */
  private static final Pattern CN_SECTION = Pattern.compile("^[一二三四五六七八九十]+[、.．]");

  /** 解析 docx 字节为小节列表;完全无内容时返回空列表(调用方置项目失败) */
  public List<SourceSection> parse(byte[] docxBytes) {
    List<SourceSection> sections = new ArrayList<>();
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
      StringBuilder text = new StringBuilder();
      String heading = null;
      int level = 0;
      String locator = "第 1 段";
      int paraIndex = 0;

      for (IBodyElement element : document.getBodyElements()) {
        if (element instanceof XWPFParagraph paragraph) {
          String paraText = paragraph.getText();
          if (paraText == null || paraText.isBlank()) {
            continue;
          }
          paraIndex++;
          Integer headingLevel = headingLevelOf(document, paragraph);
          if (headingLevel != null) {
            // flush 前一小节
            if (heading != null || !text.toString().isBlank()) {
              sections.add(new SourceSection(null, heading, level, text.toString().trim(), locator));
            }
            heading = paraText.trim();
            level = headingLevel;
            locator = "第 " + paraIndex + " 段";
            text = new StringBuilder();
          } else {
            if (heading == null && text.length() == 0) {
              locator = "第 " + paraIndex + " 段";
            }
            text.append(paraText.trim()).append('\n');
          }
        } else if (element instanceof XWPFTable table) {
          paraIndex++;
          if (heading == null && text.length() == 0) {
            locator = "第 " + paraIndex + " 段";
          }
          String tableText = tableToText(table);
          if (!tableText.isBlank()) {
            text.append(tableText).append('\n');
          }
        }
      }
      if (heading != null || !text.toString().isBlank()) {
        sections.add(new SourceSection(null, heading, level, text.toString().trim(), locator));
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("docx 解析失败: " + e.getMessage(), e);
    }
    return sections;
  }

  /** 标题级别:样式名优先,编号正则兜底;均不命中返回 null(供小节切分与知识点枚举复用) */
  public Integer headingLevelOf(XWPFDocument document, XWPFParagraph paragraph) {
    String styleId = paragraph.getStyleID();
    if (styleId != null) {
      XWPFStyle style = document.getStyles().getStyle(styleId);
      String styleName = style != null ? style.getName() : null;
      if (styleName != null) {
        Matcher m = HEADING_STYLE.matcher(styleName.trim());
        if (m.matches()) {
          int lvl = Integer.parseInt(m.group(1));
          if (lvl >= 1 && lvl <= 6) {
            return lvl;
          }
        }
      }
    }
    String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
    if (text.isEmpty()) {
      return null;
    }
    if (CN_CHAPTER.matcher(text).find()) {
      return 1;
    }
    Matcher num = NUM_SECTION.matcher(text);
    if (num.find()) {
      String numbering = num.group(1);
      int dots = numbering.length() - numbering.replace(".", "").length();
      return Math.min(dots + 1, 6);
    }
    if (CN_SECTION.matcher(text).find()) {
      return 2;
    }
    return null;
  }

  /** 表格 → 单元格 trim 后 " | " 连接成行,行间换行 */
  private String tableToText(XWPFTable table) {
    StringBuilder sb = new StringBuilder();
    for (XWPFTableRow row : table.getRows()) {
      StringBuilder line = new StringBuilder();
      for (XWPFTableCell cell : row.getTableCells()) {
        String cellText = cell.getText() == null ? "" : cell.getText().replaceAll("\\s+", " ").trim();
        if (line.length() > 0) {
          line.append(" | ");
        }
        line.append(cellText);
      }
      if (!line.toString().isBlank()) {
        sb.append(line).append('\n');
      }
    }
    return sb.toString().trim();
  }
}
