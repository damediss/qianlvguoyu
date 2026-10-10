package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.util.StringUtils;

/**
 * 补贴 Word 轻量编辑：解析可改段落 / 表格单元格（标题 / 标签行 / 图注），按索引写回，保留图片与样式。
 *
 * <p>表格单元格索引编码为 {@code 1_000_000 + table*10_000 + row*100 + col}，与正文段落索引区分。
 */
public final class SubsidyDocxTextPatcher {

  public record Line(int index, String kind, String label, String value, String text) {}

  public record ParseResult(List<Line> lines, int paragraphCount) {}

  /** 表格单元格索引基址（正文段落索引通常远小于此值） */
  public static final int TABLE_INDEX_BASE = 1_000_000;

  private static final Pattern FIGURE = Pattern.compile("^图\\s*(\\d+)\\s*[:：]\\s*(.*)$");

  /** 长标签优先；覆盖申请表常见栏位 */
  private static final List<String> META_LABELS;

  static {
    List<String> labels = new ArrayList<>();
    labels.add("企业组织培训企业名称");
    labels.add("企业组织培训企业联系电话");
    labels.add("培训机构名称");
    labels.add("培训单位名称");
    labels.add("培训单位（盖章）");
    labels.add("培训学校名称");
    labels.add("培训机构");
    labels.add("培训单位");
    labels.add("培训学校");
    labels.add("申报单位");
    labels.add("企业名称");
    labels.add("经办人姓名");
    labels.add("经办人");
    labels.add("联系人电话");
    labels.add("联系电话");
    labels.add("联系人");
    labels.add("本次培训班次");
    labels.add("培训班次");
    labels.add("班期编号");
    labels.add("班期名称");
    labels.add("培训起止时间");
    labels.add("培训时间");
    labels.add("培训地址");
    labels.add("培训地点");
    labels.add("培训专业类别");
    labels.add("培训工种");
    labels.add("培训级别");
    labels.add("工种");
    labels.add("补贴总人数");
    labels.add("补贴总金额");
    labels.add("申请补贴金额");
    labels.add("补贴金额");
    labels.add("申报日期");
    labels.add("申请日期");
    labels.sort(Comparator.comparingInt(String::length).reversed());
    META_LABELS = List.copyOf(labels);
  }

  private SubsidyDocxTextPatcher() {}

  public static int encodeTableCell(int tableIdx, int rowIdx, int colIdx) {
    return TABLE_INDEX_BASE + tableIdx * 10_000 + rowIdx * 100 + colIdx;
  }

  public static boolean isTableIndex(int index) {
    return index >= TABLE_INDEX_BASE;
  }

  public static int[] decodeTableCell(int index) {
    int v = index - TABLE_INDEX_BASE;
    int tableIdx = v / 10_000;
    int rem = v % 10_000;
    int rowIdx = rem / 100;
    int colIdx = rem % 100;
    return new int[] {tableIdx, rowIdx, colIdx};
  }

  public static ParseResult parse(byte[] docxBytes) throws IOException {
    if (docxBytes == null || docxBytes.length == 0) {
      throw new IOException("空 Word 文件");
    }
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
      List<Line> lines = new ArrayList<>();
      Set<String> seenLabels = new LinkedHashSet<>();
      boolean titleTaken = false;

      // 正文段落（与 doc.getParagraphs() 同序，供 patch 按索引写回）
      List<XWPFParagraph> paragraphs = doc.getParagraphs();
      for (int i = 0; i < paragraphs.size(); i++) {
        XWPFParagraph p = paragraphs.get(i);
        if (p == null || paragraphHasDrawing(p)) {
          continue;
        }
        String text = safeText(p);
        if (!StringUtils.hasText(text)) {
          continue;
        }
        Line labeled = matchLabeledLine(i, text.trim());
        if (labeled != null) {
          if (seenLabels.add(labeled.label())) {
            lines.add(labeled);
          }
          continue;
        }
        Matcher fig = FIGURE.matcher(text.trim());
        if (fig.matches()) {
          lines.add(new Line(i, "caption", "图" + fig.group(1), fig.group(2), text.trim()));
          continue;
        }
        if (!titleTaken && text.trim().length() >= 4) {
          lines.add(new Line(i, "title", "标题", text.trim(), text.trim()));
          titleTaken = true;
        }
      }

      // 表格：同格「标签：值」或「标签格 + 右侧值格」
      List<XWPFTable> tables = doc.getTables();
      for (int t = 0; t < tables.size(); t++) {
        XWPFTable table = tables.get(t);
        if (table == null) {
          continue;
        }
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 0; r < rows.size(); r++) {
          XWPFTableRow row = rows.get(r);
          if (row == null) {
            continue;
          }
          List<XWPFTableCell> cells = row.getTableCells();
          for (int c = 0; c < cells.size(); c++) {
            XWPFTableCell cell = cells.get(c);
            String cellText = cellText(cell).trim();
            if (!StringUtils.hasText(cellText)) {
              continue;
            }
            Line sameCell = matchLabeledLine(encodeTableCell(t, r, c), cellText);
            if (sameCell != null && seenLabels.add(sameCell.label())) {
              lines.add(sameCell);
              continue;
            }
            String pureLabel = stripTrailingColon(cellText);
            if (isKnownLabel(pureLabel) && c + 1 < cells.size()) {
              XWPFTableCell valueCell = cells.get(c + 1);
              String valueText = cellText(valueCell);
              // 值格若本身又是标签则跳过（避免把下一标签当值）
              if (isKnownLabel(stripTrailingColon(valueText.trim()))) {
                continue;
              }
              if (seenLabels.add(pureLabel)) {
                int valueIdx = encodeTableCell(t, r, c + 1);
                lines.add(
                    new Line(
                        valueIdx,
                        "meta",
                        pureLabel,
                        valueText == null ? "" : valueText.trim(),
                        valueText == null ? "" : valueText.trim()));
              }
            }
          }
        }
      }

      return new ParseResult(List.copyOf(lines), paragraphs.size());
    }
  }

  /**
   * @param updates key=段落索引或表格编码索引，value=整段/整格新文本
   */
  public static byte[] patch(byte[] docxBytes, Map<Integer, String> updates) throws IOException {
    if (docxBytes == null || docxBytes.length == 0) {
      throw new IOException("空 Word 文件");
    }
    if (updates == null || updates.isEmpty()) {
      return docxBytes;
    }
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      List<XWPFParagraph> paragraphs = doc.getParagraphs();
      List<XWPFTable> tables = doc.getTables();
      for (Map.Entry<Integer, String> e : updates.entrySet()) {
        Integer idx = e.getKey();
        if (idx == null) {
          continue;
        }
        String next = e.getValue() == null ? "" : e.getValue();
        if (isTableIndex(idx)) {
          int[] coord = decodeTableCell(idx);
          if (coord[0] < 0 || coord[0] >= tables.size()) {
            continue;
          }
          XWPFTable table = tables.get(coord[0]);
          if (table == null || coord[1] < 0 || coord[1] >= table.getNumberOfRows()) {
            continue;
          }
          XWPFTableRow row = table.getRow(coord[1]);
          if (row == null || coord[2] < 0 || coord[2] >= row.getTableCells().size()) {
            continue;
          }
          replaceCellTextKeepStyle(row.getCell(coord[2]), next);
        } else {
          if (idx < 0 || idx >= paragraphs.size()) {
            continue;
          }
          XWPFParagraph p = paragraphs.get(idx);
          if (p == null || paragraphHasDrawing(p)) {
            continue;
          }
          replaceParagraphTextKeepStyle(p, next);
        }
      }
      doc.write(out);
      return out.toByteArray();
    }
  }

  /** 按 label 写值：培训工种 → 「培训工种：xxx」；表格值格只写值本身 */
  public static byte[] patchByLabels(byte[] docxBytes, Map<String, String> labelValues)
      throws IOException {
    if (labelValues == null || labelValues.isEmpty()) {
      return docxBytes;
    }
    ParseResult parsed = parse(docxBytes);
    Map<Integer, String> updates = new LinkedHashMap<>();
    for (Line line : parsed.lines()) {
      if ("title".equals(line.kind())) {
        String title = first(labelValues, "标题", "title");
        if (title != null) {
          updates.put(line.index(), title);
        }
        continue;
      }
      if ("meta".equals(line.kind())) {
        String v = first(labelValues, line.label(), mapMetaKey(line.label()));
        if (v != null) {
          if (isTableIndex(line.index())) {
            // 表格「标签|值」分格：值格只写 value
            String original = line.text() == null ? "" : line.text();
            boolean sameCellLabeled =
                original.contains("：") || original.contains(":") || original.startsWith(line.label());
            if (sameCellLabeled && (original.contains("：") || original.contains(":"))) {
              updates.put(line.index(), line.label() + "：" + v);
            } else {
              updates.put(line.index(), v);
            }
          } else {
            updates.put(line.index(), line.label() + "：" + v);
          }
        }
        continue;
      }
      if ("caption".equals(line.kind())) {
        String v = first(labelValues, line.label(), "caption:" + line.label());
        if (v != null) {
          String trimmed = v.trim();
          if (trimmed.startsWith("图")) {
            updates.put(line.index(), trimmed);
          } else {
            updates.put(line.index(), line.label() + "：" + trimmed);
          }
        }
      }
    }
    return patch(docxBytes, updates);
  }

  private static Line matchLabeledLine(int index, String text) {
    // 纯标签格（无冒号）交给「标签|值」分格逻辑，避免「经办人姓名」被拆成「经办人」+「姓名」
    if (!text.contains("：") && !text.contains(":") && isKnownLabel(text.trim())) {
      return null;
    }
    for (String label : META_LABELS) {
      if (text.startsWith(label)) {
        String rest = text.substring(label.length()).trim();
        if (rest.startsWith("：") || rest.startsWith(":")) {
          rest = rest.substring(1).trim();
          return new Line(index, "meta", label, rest, text);
        }
        // 「培训机构广西…」无冒号但标签后紧跟内容
        if (!rest.isEmpty() && !isKnownLabel(rest)) {
          return new Line(index, "meta", label, rest, text);
        }
      }
    }
    return null;
  }

  private static boolean isKnownLabel(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = stripTrailingColon(text.trim());
    for (String label : META_LABELS) {
      if (label.equals(t)) {
        return true;
      }
    }
    return false;
  }

  private static String stripTrailingColon(String text) {
    if (text == null) {
      return "";
    }
    String t = text.trim();
    if (t.endsWith("：") || t.endsWith(":")) {
      return t.substring(0, t.length() - 1).trim();
    }
    return t;
  }

  private static String mapMetaKey(String label) {
    return switch (label) {
      case "培训工种", "培训专业类别", "工种" -> "worktype";
      case "培训级别" -> "level";
      case "培训时间", "培训起止时间" -> "trainTime";
      case "培训单位", "培训单位名称", "培训机构", "培训机构名称", "培训学校", "培训学校名称", "培训单位（盖章）" ->
          "trainUnit";
      case "培训地址", "培训地点" -> "trainAddress";
      case "经办人", "经办人姓名" -> "fzr";
      case "联系电话", "联系人电话" -> "fzrtel";
      case "培训班次", "本次培训班次", "班期编号", "班期名称" -> "class_title";
      case "企业名称", "申报单位" -> "company";
      default -> label;
    };
  }

  private static String first(Map<String, String> map, String... keys) {
    for (String k : keys) {
      if (k != null && map.containsKey(k)) {
        return map.get(k) == null ? "" : map.get(k);
      }
    }
    return null;
  }

  private static boolean paragraphHasDrawing(XWPFParagraph p) {
    try {
      return p.getCTP() != null
          && p.getCTP().getRList() != null
          && p.getCTP().getRList().stream()
              .anyMatch(r -> r.sizeOfDrawingArray() > 0 || r.sizeOfPictArray() > 0);
    } catch (Exception ex) {
      return false;
    }
  }

  private static String safeText(XWPFParagraph p) {
    try {
      String t = p.getText();
      return t == null ? "" : t;
    } catch (Exception ex) {
      return "";
    }
  }

  private static String cellText(XWPFTableCell cell) {
    if (cell == null) {
      return "";
    }
    try {
      String t = cell.getText();
      return t == null ? "" : t;
    } catch (Exception ex) {
      return "";
    }
  }

  /** 清空 runs 后写入新文本，尽量复用首个 run 的字体样式 */
  private static void replaceParagraphTextKeepStyle(XWPFParagraph p, String text) {
    List<XWPFRun> runs = p.getRuns();
    XWPFRun styleSrc = runs != null && !runs.isEmpty() ? runs.get(0) : null;
    String font = styleSrc != null ? styleSrc.getFontFamily() : null;
    int fontSize = styleSrc != null ? styleSrc.getFontSize() : -1;
    boolean bold = styleSrc != null && styleSrc.isBold();

    for (int i = runs.size() - 1; i >= 0; i--) {
      p.removeRun(i);
    }
    XWPFRun run = p.createRun();
    if (StringUtils.hasText(font)) {
      run.setFontFamily(font);
      run.setFontFamily(font, XWPFRun.FontCharRange.eastAsia);
    }
    if (fontSize > 0) {
      run.setFontSize(fontSize);
    }
    run.setBold(bold);
    run.setText(text == null ? "" : text);
  }

  private static void replaceCellTextKeepStyle(XWPFTableCell cell, String text) {
    if (cell == null) {
      return;
    }
    List<XWPFParagraph> paras = cell.getParagraphs();
    if (paras == null || paras.isEmpty()) {
      XWPFParagraph p = cell.addParagraph();
      replaceParagraphTextKeepStyle(p, text);
      return;
    }
    // 只改第一段；清空多余段文本，避免残留
    replaceParagraphTextKeepStyle(paras.get(0), text);
    for (int i = 1; i < paras.size(); i++) {
      replaceParagraphTextKeepStyle(paras.get(i), "");
    }
  }
}
