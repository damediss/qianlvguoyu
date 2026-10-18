package com.guoyu.platform.exam.zhenti.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件名 → 元数据（纯函数，方案 §2.5 与详规 §4.1 逐条对齐）。
 *
 * <p>规则顺序：年份 → 科目 → 工种等级 → 分隔符清理 → 解析不出任一关键项则 metaPending。
 * 解析器不访问数据库、不依赖 Spring 上下文，便于单测覆盖全部边界。
 */
@Component
public class ZhentiFileNameParser {

  private static final Pattern YEAR = Pattern.compile("(19|20)\\d{2}");
  private static final Pattern LEVEL =
      Pattern.compile("^(.+?)[（(](.+?)[）)](五级|四级|三级|二级|一级)");
  private static final Pattern LEADING_YEAR = Pattern.compile(
      "^[【\\[\\(]?\\s*(19|20)\\d{2}\\s*[】\\]\\)]?\\s*[-_—\\s]+");
  private static final Pattern TRAILING_YEAR = Pattern.compile(
      "[-_—\\s]+[【\\[\\(]?\\s*(19|20)\\d{2}\\s*[】\\]\\)]?\\s*$");
  private static final String EDGE_CHARS = "-_— \t\r\n【】[]（）()";
  private static final String[] SUBJECTS = {"理论", "实操", "综合"};
  private static final int TITLE_MAX = 200;

  /**
   * @param fileName 原始文件名（可含扩展名与路径）
   * @return 解析结果；title 永不为空，metaPending=1 表示缺年份或缺工种等级
   */
  public ParsedFileName parse(String fileName) {
    String base = stripExtension(baseName(fileName));
    Matcher yearMatcher = YEAR.matcher(base);
    String year = yearMatcher.find() ? yearMatcher.group() : null;

    String subject = null;
    for (String candidate : SUBJECTS) {
      if (base.contains(candidate)) {
        subject = candidate;
        break;
      }
    }

    boolean levelParsed = LEVEL.matcher(base).find();
    String title = cleanTitle(base);
    if (!StringUtils.hasText(title)) {
      title = "未命名真题";
    }
    if (title.length() > TITLE_MAX) {
      title = title.substring(0, TITLE_MAX);
    }
    boolean metaPending = !StringUtils.hasText(year) || !levelParsed;
    return new ParsedFileName(title, year, subject, levelParsed, metaPending);
  }

  /** 清理标题：去掉首尾年份 token 与分隔符残留。 */
  String cleanTitle(String base) {
    String title = base == null ? "" : base.trim();
    title = LEADING_YEAR.matcher(title).replaceFirst("");
    title = TRAILING_YEAR.matcher(title).replaceFirst("");
    int start = 0;
    int end = title.length();
    while (start < end && EDGE_CHARS.indexOf(title.charAt(start)) >= 0) {
      start++;
    }
    while (end > start && EDGE_CHARS.indexOf(title.charAt(end - 1)) >= 0) {
      end--;
    }
    title = title.substring(start, end).trim();
    // 去掉括号包裹年份后可能残留的空白与连接符
    return title.replaceAll("^[-_—\\s]+|[-_—\\s]+$", "");
  }

  private static String baseName(String fileName) {
    if (!StringUtils.hasText(fileName)) {
      return "";
    }
    String normalized = fileName.replace('\\', '/');
    int slash = normalized.lastIndexOf('/');
    return slash >= 0 ? normalized.substring(slash + 1) : normalized;
  }

  private static String stripExtension(String name) {
    int dot = name.lastIndexOf('.');
    if (dot > 0 && dot < name.length() - 1 && name.substring(dot + 1).equalsIgnoreCase("pdf")) {
      return name.substring(0, dot);
    }
    return name;
  }

  /**
   * 解析结果。
   *
   * @param title       标题（兜底文件名主体，≤200 字）
   * @param year        年份，可能为 null
   * @param subject     科目（理论/实操/综合），可能为 null；仅展示用
   * @param levelParsed 是否解析出「工种（别名）等级」
   * @param metaPending 是否进入待完善队列
   */
  public record ParsedFileName(
      String title,
      String year,
      String subject,
      boolean levelParsed,
      boolean metaPending
  ) {
  }
}
