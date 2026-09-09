package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.util.StringUtils;

/**
 * 补贴 Word 轻量编辑：解析可改段落（标题 / 标签行 / 图注），按索引写回全文，保留图片与段落样式。
 */
public final class SubsidyDocxTextPatcher {

  public record Line(int index, String kind, String label, String value, String text) {}

  public record ParseResult(List<Line> lines, int paragraphCount) {}

  private static final Pattern LABELED =
      Pattern.compile("^(培训工种|培训级别|培训时间|培训单位|培训地址|申报单位|企业名称)\\s*[:：]\\s*(.*)$");
  private static final Pattern FIGURE =
      Pattern.compile("^图\\s*(\\d+)\\s*[:：]\\s*(.*)$");

  private SubsidyDocxTextPatcher() {}

  public static ParseResult parse(byte[] docxBytes) throws IOException {
    if (docxBytes == null || docxBytes.length == 0) {
      throw new IOException("空 Word 文件");
    }
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
      List<Line> lines = new ArrayList<>();
      List<XWPFParagraph> paragraphs = doc.getParagraphs();
      boolean titleTaken = false;
      for (int i = 0; i < paragraphs.size(); i++) {
        XWPFParagraph p = paragraphs.get(i);
        if (p == null) {
          continue;
        }
        // 含图段落不作为可编辑行（避免误改 drawing）
        if (paragraphHasDrawing(p)) {
          continue;
        }
        String text = safeText(p);
        if (!StringUtils.hasText(text)) {
          continue;
        }
        Matcher labeled = LABELED.matcher(text.trim());
        if (labeled.matches()) {
          lines.add(new Line(i, "meta", labeled.group(1), labeled.group(2), text.trim()));
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
      return new ParseResult(List.copyOf(lines), paragraphs.size());
    }
  }

  /**
   * @param updates key=段落索引，value=整段新文本
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
      for (Map.Entry<Integer, String> e : updates.entrySet()) {
        Integer idx = e.getKey();
        if (idx == null || idx < 0 || idx >= paragraphs.size()) {
          continue;
        }
        XWPFParagraph p = paragraphs.get(idx);
        if (p == null || paragraphHasDrawing(p)) {
          continue;
        }
        String next = e.getValue() == null ? "" : e.getValue();
        replaceParagraphTextKeepStyle(p, next);
      }
      doc.write(out);
      return out.toByteArray();
    }
  }

  /** 按 label 写值：培训工种 → 「培训工种：xxx」 */
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
          updates.put(line.index(), line.label() + "：" + v);
        }
        continue;
      }
      if ("caption".equals(line.kind())) {
        String v = first(labelValues, line.label(), "caption:" + line.label());
        if (v != null) {
          // 保留「图N：」前缀；若调用方已带前缀则整段替换
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

  private static String mapMetaKey(String label) {
    return switch (label) {
      case "培训工种" -> "worktype";
      case "培训级别" -> "level";
      case "培训时间" -> "trainTime";
      case "培训单位" -> "trainUnit";
      case "培训地址" -> "trainAddress";
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
}
