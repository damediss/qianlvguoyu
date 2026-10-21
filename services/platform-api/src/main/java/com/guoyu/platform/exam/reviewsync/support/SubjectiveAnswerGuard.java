package com.guoyu.platform.exam.reviewsync.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 主观题无意义作答闸口（《主观题无意义作答拦截与学员引导方案》§3.1）。
 *
 * <p>纯函数、无依赖：判分服务在缓存/限流/LLM 之前调用，命中即记 0 分、不调 LLM。
 * 判定保守——宁漏勿错，拿不准的一律放行给 LLM（误伤真实短答比浪费一次调用更糟）。
 *
 * <p>规则（归一化 = trim + 连续空白折叠）：
 * <ul>
 *   <li>豁免优先：作答命中参考答案任一 ≥2 字中文片段 → 放行（言简意赅的真实短答）；</li>
 *   <li>R1 纯数字/纯符号/纯字母（"2"、"Q"、"666"、"..."）；</li>
 *   <li>R2 无汉字且总长 &lt; 15（"asdf"、"abc123"）；</li>
 *   <li>R3 汉字数 &lt; minHanzi 且总长 &lt; 8（"不会"、"不知道"、"略"、"过"）；</li>
 *   <li>R4 单字符重复 ≥4 或 2~4 字符片段重复 ≥3（"哈哈哈哈"、"123123123"）。</li>
 * </ul>
 */
public final class SubjectiveAnswerGuard {

  /** 放行判定 */
  public static final Verdict MEANINGFUL = new Verdict(false, null);

  private static final Pattern HANZI_RUN = Pattern.compile("[\\u4e00-\\u9fff]{2,}");
  private static final Pattern PURE_DIGIT_SYMBOL = Pattern.compile("^[\\d\\p{Punct}\\s]+$");
  private static final Pattern PURE_LETTER = Pattern.compile("^[A-Za-z]+$");
  private static final Pattern SINGLE_CHAR_REPEAT = Pattern.compile("^(.)(\\1){3,}$");
  private static final Pattern SEGMENT_REPEAT = Pattern.compile("^(.{2,4})(\\1){2,}$", Pattern.DOTALL);

  private SubjectiveAnswerGuard() {}

  /** 判定结果：meaningless=true 时 reason 为命中规则（empty/noHanzi/tooShort/repeated） */
  public record Verdict(boolean meaningless, String reason) {}

  /**
   * 无意义作答判定。
   *
   * @param userAnswer     学员作答原文（null 按空串处理，上游已挡空，此处防御兜底）
   * @param referenceAnswer 提取后的参考答案（仅用于豁免匹配；null 无豁免）
   * @param minHanzi       R3 汉字数阈值（字典 ai_review_grade_guard_min_hanzi，默认 4）
   */
  public static Verdict check(String userAnswer, String referenceAnswer, int minHanzi) {
    String normalized = normalize(userAnswer);
    if (normalized.isEmpty()) {
      return new Verdict(true, "empty");
    }
    if (hitsReferenceKeyword(normalized, referenceAnswer)) {
      return MEANINGFUL;
    }
    int hanzi = countHanzi(normalized);
    if (PURE_DIGIT_SYMBOL.matcher(normalized).matches() || PURE_LETTER.matcher(normalized).matches()) {
      return new Verdict(true, "noHanzi");
    }
    if (hanzi == 0 && normalized.length() < 15) {
      return new Verdict(true, "noHanzi");
    }
    if (hanzi > 0 && hanzi < minHanzi && normalized.length() < 8) {
      return new Verdict(true, "tooShort");
    }
    if (SINGLE_CHAR_REPEAT.matcher(normalized).matches()
        || SEGMENT_REPEAT.matcher(normalized).matches()) {
      return new Verdict(true, "repeated");
    }
    return MEANINGFUL;
  }

  /**
   * 豁免：参考答案中任一 ≥2 字中文片段与作答互相包含 → 真实作答（引用要点也算）。
   * 参考答案的片段是自然语言长句，学员通常只写其中关键词（如参考答案"方向是低碳、…"，
   * 作答"低碳"），故双向包含；单字不作豁免（"低"仍按 R3 拦截）。
   */
  private static boolean hitsReferenceKeyword(String normalized, String referenceAnswer) {
    if (referenceAnswer == null || referenceAnswer.isBlank()) {
      return false;
    }
    List<String> runs = new ArrayList<>();
    Matcher m = HANZI_RUN.matcher(referenceAnswer);
    while (m.find()) {
      runs.add(m.group());
    }
    for (String run : runs) {
      if (normalized.contains(run)) {
        return true;
      }
      if (normalized.length() >= 2 && run.contains(normalized)) {
        return true;
      }
    }
    return false;
  }

  private static int countHanzi(String text) {
    int n = 0;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) >= 0x4e00 && text.charAt(i) <= 0x9fff) {
        n++;
      }
    }
    return n;
  }

  private static String normalize(String userAnswer) {
    return userAnswer == null ? "" : userAnswer.trim().replaceAll("\\s+", " ");
  }
}
