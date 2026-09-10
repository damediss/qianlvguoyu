package com.guoyu.platform.exam.theorygen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 二级/一级主观题轮转配额（指引 §2.1：5 分制题型 ≥40 题、10/15 分制题型 ≥40 题、主观分值占比 40%~60%）。
 * 背景：主观题不是"每点固定 N 道"，而是全局轮转（每 N 个考评点 1 道，同档位题型轮换）。
 */
class TheoryGenPromptBuilderSubjectiveTest {

  @Test
  void defaultQuota_level2_includesSubjectiveRotation() {
    Map<String, Integer> q = TheoryGenPromptBuilder.defaultQuota(2);
    assertEquals(3, q.get("single"));
    assertEquals(2, q.get("multi"));
    assertEquals(2, q.get("judge"));
    assertEquals(3, q.get("sub5"));
    assertEquals(3, q.get("sub10"));
  }

  @Test
  void defaultQuota_level3to5_noSubjectiveKeys() {
    for (int level : new int[]{5, 4, 3}) {
      Map<String, Integer> q = TheoryGenPromptBuilder.defaultQuota(level);
      assertFalse(q.containsKey("sub5"), "level " + level + " 不应带主观配额");
      assertFalse(q.containsKey("sub10"), "level " + level + " 不应带主观配额");
    }
  }

  @Test
  void subjectiveRotation_assignsByGlobalIndex() {
    Map<String, Integer> q = TheoryGenPromptBuilder.defaultQuota(2);
    // 间隔 3：序号 0 命中 → 5 分制首型简答 + 10 分制首型论述
    assertEquals(List.of("essay", "discuss"), TheoryGenPromptBuilder.subjectiveTypesForPoint(0, q));
    assertEquals(List.of(), TheoryGenPromptBuilder.subjectiveTypesForPoint(1, q));
    assertEquals(List.of(), TheoryGenPromptBuilder.subjectiveTypesForPoint(2, q));
    // 序号 3：5 分制轮计算、10 分制轮案例分析
    assertEquals(List.of("cal", "case_analysis"), TheoryGenPromptBuilder.subjectiveTypesForPoint(3, q));
    // 序号 6：5 分制轮绘图、10 分制回到论述
    assertEquals(List.of("draw", "discuss"), TheoryGenPromptBuilder.subjectiveTypesForPoint(6, q));
    // 序号 9：5 分制回到简答、10 分制轮案例分析
    assertEquals(List.of("essay", "case_analysis"), TheoryGenPromptBuilder.subjectiveTypesForPoint(9, q));
  }

  @Test
  void subjectiveRotation_nullAndMissingKeys() {
    assertTrue(TheoryGenPromptBuilder.subjectiveTypesForPoint(null, Map.of("sub5", 3)).isEmpty());
    assertTrue(TheoryGenPromptBuilder.subjectiveTypesForPoint(0, null).isEmpty());
    assertTrue(TheoryGenPromptBuilder.subjectiveTypesForPoint(0, Map.of("single", 3)).isEmpty());
  }

  @Test
  void subjectiveRotation_120PointsHitsFortyEach() {
    Map<String, Integer> q = TheoryGenPromptBuilder.defaultQuota(2);
    long sub5Count = 0;
    long sub10Count = 0;
    for (int i = 0; i < 120; i++) {
      List<String> types = TheoryGenPromptBuilder.subjectiveTypesForPoint(i, q);
      sub5Count += types.stream().filter(t -> List.of("essay", "cal", "draw").contains(t)).count();
      sub10Count += types.stream().filter(t -> List.of("discuss", "case_analysis").contains(t)).count();
    }
    assertEquals(40, sub5Count, "5 分制主观题 120 点 → 40 道（指引下限）");
    assertEquals(40, sub10Count, "10/15 分制主观题 120 点 → 40 道（指引下限）");
  }

  @Test
  void userPrompt_marksSubjectiveTasksExplicitly() {
    TheoryBlueprintNodeEntity p = new TheoryBlueprintNodeEntity();
    p.setFullCode("A-A-A-001");
    p.setName("职业道德基本知识");
    Map<String, Integer> q = TheoryGenPromptBuilder.defaultQuota(2);

    String without = TheoryGenPromptBuilder.buildUserPrompt(p, q);
    assertFalse(without.contains("额外生成"));

    String with = TheoryGenPromptBuilder.buildUserPrompt(p, q, List.of("essay", "discuss"));
    assertTrue(with.contains("简答题 1 道"));
    assertTrue(with.contains("论述题 1 道"));
    assertTrue(with.contains("参考答案"));
  }
}
