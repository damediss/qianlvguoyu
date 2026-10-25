package com.guoyu.platform.textbook.domain;

import com.guoyu.platform.textbook.vo.TextbookCheckReportVO;
import com.guoyu.platform.textbook.vo.TextbookCheckReportVO.UncoveredPoint;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 确定性内容检查（方案 §8.3，非 LLM）：字数区间、可能未覆盖的标准点、待补充占位计数。
 *
 * <p>全部结果仅为提示：除「空正文」和 final 导出占位外不阻止提交/通过（方案 §8.3）。
 * 覆盖检查取该节直接子级全部理论/技能点，去除编号、空白和常见标点后做完整标题包含匹配；
 * 未命中只表示「可能未覆盖」，不伪称语义覆盖判定。
 */
public final class TextbookContentChecker {

  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /** 标准点输入（title_raw 优先，回退 title）。 */
  public record StandardPoint(Long pointId, String title, String dimension) {
  }

  private TextbookContentChecker() {
  }

  public static TextbookCheckReportVO check(String contentMd, Integer targetWords,
      List<StandardPoint> points) {
    int wordCount = TextbookMarkdownSupport.wordCount(contentMd);
    Integer min = null;
    Integer max = null;
    Boolean within = null;
    if (targetWords != null && targetWords > 0) {
      min = (int) Math.floor(targetWords * 0.8);
      max = (int) Math.ceil(targetWords * 1.2);
      within = wordCount >= min && wordCount <= max;
    }
    String plain = normalizeForCoverage(TextbookMarkdownSupport.toPlainText(contentMd));
    List<UncoveredPoint> uncovered = new ArrayList<>();
    if (points != null) {
      for (StandardPoint point : points) {
        String title = normalizeForCoverage(point.title() == null ? "" : point.title());
        if (title.isEmpty()) {
          continue;
        }
        if (!plain.contains(title)) {
          uncovered.add(new UncoveredPoint(
              point.pointId() == null ? null : String.valueOf(point.pointId()),
              point.title(), point.dimension()));
        }
      }
    }
    int placeholders = TextbookMarkdownSupport.placeholderCount(contentMd);
    return new TextbookCheckReportVO(wordCount, targetWords, min, max, within,
        List.copyOf(uncovered), placeholders, LocalDateTime.now().format(FORMAT));
  }

  /** 去除编号、空白与常见标点，做完整标题包含匹配的归一化口径。 */
  static String normalizeForCoverage(String text) {
    if (text == null) {
      return "";
    }
    return text.replaceAll("[\\s\\p{Punct}，。、；：？！“”‘’（）《》【】〔〕—…·「」『』]", "");
  }
}
