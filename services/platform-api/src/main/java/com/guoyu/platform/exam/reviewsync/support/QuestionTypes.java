package com.guoyu.platform.exam.reviewsync.support;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

/**
 * 题型口径唯一来源（《简答题作答与AI判分方案》Q1 白名单）。
 *
 * <p>判分（AiGradeApplicationService）、选题池过滤（ReviewQuestionQueryService）、
 * 组卷裁剪（QuestionSelectDomainService）必须引用本类，禁止各处私有复制
 * （AGENTS.md：同一概念出现第二份实现即视为缺陷）。
 */
public final class QuestionTypes {

  private QuestionTypes() {
  }

  /** 客观三型（List 保序：SQL IN 与断言口径稳定） */
  public static final List<String> OBJECTIVE_TYPES = List.of("single", "multi", "judge");

  /** 主观题白名单 8 种（List 保序）；空 type 与未知 type 不按主观题处理 */
  public static final List<String> SUBJECTIVE_TYPES =
      List.of("answer", "cal", "essay", "analysis", "explain", "fill", "short", "drafting");

  /** 同步学选题池入池题型：客观三型 + 主观题 8 白名单（保序） */
  public static final List<String> SCOPE_TYPES =
      Stream.concat(OBJECTIVE_TYPES.stream(), SUBJECTIVE_TYPES.stream()).toList();

  /** 主观题判定（trim + 小写归一）；内部走 Set 查询 */
  public static boolean isSubjective(String type) {
    return type != null && SUBJECTIVE_TYPE_SET.contains(type.trim().toLowerCase());
  }

  private static final Set<String> SUBJECTIVE_TYPE_SET = Set.copyOf(SUBJECTIVE_TYPES);

  /**
   * 题型归一码（《试题解析Markdown表格化_一期实施方案》§5.2）：null/blank → null；
   * 未知题型 → trim 后小写原样返回（与 FollowTaskBankQuestionConverter.normalizeType 同语义，
   * 判分/解析链路统一走本类，禁止再私有复制）。
   */
  public static String normalize(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "single", "danxuan", "单选", "单选题", "1" -> "single";
      case "multi", "duoxuan", "多选", "多选题", "2" -> "multi";
      case "judge", "pandan", "panduan", "判断", "判断题", "3" -> "judge";
      case "short", "jianda", "简答", "简答题", "4" -> "short";
      case "answer", "问答", "问答题" -> "answer";
      case "cal", "计算", "计算题" -> "cal";
      case "essay", "论述", "论述题" -> "essay";
      case "analysis", "分析", "分析题" -> "analysis";
      case "explain", "解释", "解释题" -> "explain";
      case "fill", "填空", "填空题" -> "fill";
      case "drafting", "绘图", "绘图题" -> "drafting";
      default -> t;
    };
  }

  /** 中文题型标签（prompt 展示用）；未知/空 → "未标注题型"（§5.2） */
  public static String toLabel(String raw) {
    String n = normalize(raw);
    if (n == null) {
      return "未标注题型";
    }
    return switch (n) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      case "short" -> "简答题";
      case "answer" -> "问答题";
      case "cal" -> "计算题";
      case "essay" -> "论述题";
      case "analysis" -> "分析题";
      case "explain" -> "解释题";
      case "fill" -> "填空题";
      case "drafting" -> "绘图题";
      default -> "未标注题型";
    };
  }
}
