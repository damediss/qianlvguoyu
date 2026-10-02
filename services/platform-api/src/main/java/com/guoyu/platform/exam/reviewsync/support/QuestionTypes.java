package com.guoyu.platform.exam.reviewsync.support;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
}
