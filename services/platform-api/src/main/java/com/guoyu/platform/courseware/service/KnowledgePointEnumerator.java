package com.guoyu.platform.courseware.service;

import com.guoyu.platform.courseware.entity.CwKnowledgePointEntity;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

/**
 * 知识点枚举器(方案 v1.4):基本要求按三级编号段落切;工作要求按表格
 * 「技能要求/相关知识要求」列内 x.y.z 条目切。确定性枚举,不依赖模型(防漏点)。
 * 来源 ref 与 DocxSectionParser 的小节切分逻辑严格同源(flush 镜像)。
 */
@Component
public class KnowledgePointEnumerator {

  /** 章边界:N. 基本要求 / N. 工作要求(编号与标题间允许任意空格) */
  private static final Pattern BASIC_CHAPTER = Pattern.compile("^(\\d+)\\s*\\.\\s*基本要求");
  private static final Pattern WORK_CHAPTER = Pattern.compile("^(\\d+)\\s*\\.\\s*工作要求");
  private static final Pattern ANY_CHAPTER = Pattern.compile("^(\\d+)\\s*\\.\\s*\\S");

  private final DocxSectionParser docxParser;

  public KnowledgePointEnumerator(DocxSectionParser docxParser) {
    this.docxParser = docxParser;
  }

  /** 枚举全部知识点;无则返回空列表(常规文档走原概括页路径) */
  public List<CwKnowledgePointEntity> enumerate(byte[] docxBytes) {
    List<CwKnowledgePointEntity> points = new ArrayList<>();
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
      int[] sortNo = {0};
      int sectionCount = 0;      // 已 flush 的小节数(与 DocxSectionParser 镜像)
      String pendingHeading = null; // 当前未 flush 小节是否已有标题
      boolean pendingContent = false;
      String currentRef = null;

      String mode = null;        // basic / work / null(章外)
      String jobFunction = null; // 基本要求所属节(2.x)
      StringBuilder basicText = new StringBuilder();
      CwKnowledgePointEntity openBasic = null;
      String currentLevel = null; // 工作要求等级(3.x)

      for (IBodyElement element : document.getBodyElements()) {
        if (element instanceof XWPFParagraph paragraph) {
          String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
          if (text.isEmpty()) {
            continue;
          }
          boolean heading = docxParser.headingLevelOf(document, paragraph) != null;
          if (heading) {
            // flush 镜像:上一小节有内容则计数(与小节切分一致)
            if (pendingHeading != null || pendingContent) {
              sectionCount++;
            }
            pendingHeading = text;
            pendingContent = false;
            currentRef = "S1:" + (sectionCount + 1);

            // 章边界判定
            Matcher bm = BASIC_CHAPTER.matcher(text);
            Matcher wm = WORK_CHAPTER.matcher(text);
            if (bm.find()) {
              mode = "basic";
              jobFunction = null;
            } else if (wm.find()) {
              mode = "work";
              currentLevel = null;
            } else if (ANY_CHAPTER.matcher(text).find() && topLevelOf(text) == 1) {
              mode = null;
            }

            if ("basic".equals(mode)) {
              // 基本要求内部:三级编号开新点,二级编号仅改所属节
              int depth = numberedDepth(text);
              if (depth >= 3) {
                openBasic = closeBasic(openBasic, basicText, points, sortNo);
                basicText = new StringBuilder();
                openBasic = newBasicPoint(text, jobFunction, currentRef);
              } else if (depth == 2) {
                openBasic = closeBasic(openBasic, basicText, points, sortNo);
                basicText = new StringBuilder();
                jobFunction = stripNumber(text);
              } else {
                openBasic = closeBasic(openBasic, basicText, points, sortNo);
                basicText = new StringBuilder();
              }
            } else if ("work".equals(mode) && numberedDepth(text) == 2) {
              // 工作要求等级行:"3. 1 五级/初级工"
              currentLevel = stripNumber(text);
            }
          } else {
            pendingContent = true;
            if ("basic".equals(mode) && openBasic != null) {
              basicText.append(text).append('\n');
            }
          }
        } else if (element instanceof XWPFTable table) {
          pendingContent = true;
          if ("work".equals(mode)) {
            enumerateWorkTable(table, currentLevel, currentRef, points, sortNo);
          }
        }
      }
      closeBasic(openBasic, basicText, points, sortNo);
    } catch (IOException e) {
      throw new IllegalArgumentException("知识点枚举解析失败: " + e.getMessage(), e);
    }
    return points;
  }

  // ──────────────── 基本要求 ────────────────

  /** 编号深度:2. → 1;2.1 → 2;2.1.1 → 3(编号数字与点之间允许空格) */
  public static int numberedDepth(String text) {
    String t = text.trim();
    if (t.matches("^\\d+\\s*\\.\\s*\\d+\\s*\\.\\s*\\d+.*")) {
      return 3;
    }
    if (t.matches("^\\d+\\s*\\.\\s*\\d+.*")) {
      return 2;
    }
    if (t.matches("^\\d+\\s*\\.\\s*\\S.*")) {
      return 1;
    }
    return 0;
  }

  /** 章边界判定的"顶层"判断:2. 基本要求 这种单级编号 */
  private static int topLevelOf(String text) {
    return numberedDepth(text) == 1 ? 1 : 0;
  }

  /** 去掉编号取标题:"2. 1. 1 职业道德基本知识" → "职业道德基本知识" */
  public static String stripNumber(String text) {
    return text.trim()
        .replaceAll("^\\d+\\s*\\.\\s*\\d+\\s*\\.\\s*\\d+\\s*", "")
        .replaceAll("^\\d+\\s*\\.\\s*\\d+\\s*", "")
        .replaceAll("^\\d+\\s*\\.\\s*", "")
        .trim();
  }

  /** 取编号:"2. 1. 1 xxx" → "2.1.1" */
  static String extractCode(String text) {
    Matcher m = Pattern.compile("^(\\d+)\\s*\\.\\s*(\\d+)\\s*\\.\\s*(\\d+)").matcher(text.trim());
    if (m.find()) {
      return m.group(1) + "." + m.group(2) + "." + m.group(3);
    }
    return "";
  }

  private CwKnowledgePointEntity newBasicPoint(String headingText, String jobFunction,
                                               String currentRef) {
    CwKnowledgePointEntity point = new CwKnowledgePointEntity();
    point.setCategory(CwKnowledgePointEntity.CATEGORY_BASIC);
    point.setJobFunction(jobFunction);
    point.setCode(extractCode(headingText));
    point.setTitle(stripNumber(headingText));
    point.setSourceRef(currentRef);
    return point;
  }

  private CwKnowledgePointEntity closeBasic(CwKnowledgePointEntity open, StringBuilder text,
                                            List<CwKnowledgePointEntity> points, int[] sortNo) {
    if (open != null) {
      open.setKnowledgeText(text.toString().trim());
      open.setSortNo(sortNo[0]++);
      points.add(open);
    }
    return null;
  }

  // ──────────────── 工作要求 ────────────────

  /** 等级行标题:"3. 1 五级/初级工" → 取 "五级/初级工"(编号为 工作要求章内 x.y 二级) */
  private static String levelOf(String text) {
    return stripNumber(text);
  }

  /**
   * 工作要求表:列固定为 职业功能 | 工作内容(|工作内容) | 技能要求 | 相关知识要求。
   * 相关知识要求列按 x.y.z 条目逐条出点;技能要求列按编号配对挂接。
   * 注意:表头/行可能含 gridSpan 合并(如 工作内容 占两格、"二选一"合并),
   * POI 按物理单元格返回会错位,必须先按 gridSpan 展开为逻辑列再按表头定位。
   */
  void enumerateWorkTable(XWPFTable table, String currentLevel, String currentRef,
                          List<CwKnowledgePointEntity> points, int[] sortNo) {
    if (table.getRows().isEmpty()) {
      return;
    }
    List<String> header = logicalCells(table.getRow(0));
    int skillCol = -1;
    int knowledgeCol = -1;
    for (int c = 0; c < header.size(); c++) {
      String h = normalizeSpaces(header.get(c));
      if (h.contains("技能要求")) {
        skillCol = c;
      }
      if (h.contains("知识")) {
        knowledgeCol = c;
      }
    }
    if (skillCol < 0 || knowledgeCol < 0) {
      return;
    }
    int contentCol = Math.max(1, skillCol - 1);
    for (int r = 1; r < table.getRows().size(); r++) {
      List<String> cells = logicalCells(table.getRows().get(r));
      if (cells.size() <= knowledgeCol) {
        continue;
      }
      String jobFunction = normalizeSpaces(cells.get(0));
      String jobContent = normalizeSpaces(cells.get(contentCol));
      Map<String, String> skillByCode = splitXyzItems(cells.get(skillCol));
      Map<String, String> knowByCode = splitXyzItems(cells.get(knowledgeCol));
      for (Map.Entry<String, String> entry : knowByCode.entrySet()) {
        CwKnowledgePointEntity point = new CwKnowledgePointEntity();
        point.setCategory(CwKnowledgePointEntity.CATEGORY_WORK);
        point.setLevelName(currentLevel);
        point.setJobFunction(jobFunction);
        point.setJobContent(jobContent);
        point.setCode(entry.getKey());
        point.setTitle(entry.getValue());
        point.setSkillText(skillByCode.get(entry.getKey()));
        point.setKnowledgeText(entry.getValue());
        point.setSourceRef(currentRef);
        point.setSortNo(sortNo[0]++);
        points.add(point);
      }
    }
  }

  /** 行 → 逻辑列文本(按 gridSpan 展开;vMerge 续行格文本为空时原样保留空串) */
  static List<String> logicalCells(XWPFTableRow row) {
    List<String> out = new ArrayList<>();
    for (XWPFTableCell cell : row.getTableCells()) {
      int span = 1;
      try {
        if (cell.getCTTc().getTcPr() != null && cell.getCTTc().getTcPr().getGridSpan() != null) {
          span = cell.getCTTc().getTcPr().getGridSpan().getVal().intValue();
        }
      } catch (Exception ignored) {
        span = 1;
      }
      String text = cell.getText() == null ? "" : cell.getText();
      for (int i = 0; i < span; i++) {
        out.add(text);
      }
    }
    return out;
  }


  /** 单元格内 x.y.z 条目切分:code → 标题(编号内空格先归一,再按编号前瞻切) */
  public static Map<String, String> splitXyzItems(String cellText) {
    Map<String, String> out = new LinkedHashMap<>();
    if (cellText == null || cellText.isBlank()) {
      return out;
    }
    // 归一:数字与点之间的空格(1. 1. 2 → 1.1.2),其余空白压成单空格
    String t = cellText.trim()
        .replaceAll("(\\d)\\s+\\.", "$1.")
        .replaceAll("\\.\\s+(\\d)", ".$1")
        .replaceAll("[ \\t　]+", " ")
        .replaceAll("\\s*\\n\\s*", " ");
    Matcher m = Pattern.compile("(\\d+\\.\\d+\\.\\d+)\\s*").matcher(t);
    List<int[]> spans = new ArrayList<>();
    while (m.find()) {
      spans.add(new int[] {m.start(), m.end(), out.size()});
    }
    for (int i = 0; i < spans.size(); i++) {
      int contentStart = spans.get(i)[1];
      int contentEnd = i + 1 < spans.size() ? spans.get(i + 1)[0] : t.length();
      String code = t.substring(spans.get(i)[0], spans.get(i)[1]).trim();
      String title = t.substring(contentStart, contentEnd).trim();
      if (!title.isEmpty()) {
        out.put(code, title);
      }
    }
    return out;
  }

  private static String normalizeSpaces(String text) {
    return text == null ? "" : text.replaceAll("\\s+", " ").trim();
  }

  private static String cellText(XWPFTableCell cell) {
    return cell == null || cell.getText() == null ? "" : cell.getText();
  }

  /** 提取工作要求等级标题(供流水线按等级分章);非等级行返回 null */
  public static String extractWorkLevel(String text) {
    if (text == null) {
      return null;
    }
    String t = text.trim();
    if (t.matches("^\\d+\\s*\\.\\s*\\d+\\s*\\S.*")) {
      return levelOf(t);
    }
    return null;
  }
}
