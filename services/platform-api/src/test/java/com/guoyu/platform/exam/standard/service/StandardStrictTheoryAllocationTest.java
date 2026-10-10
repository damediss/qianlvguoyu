package com.guoyu.platform.exam.standard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 二/一级理论通道"考评点占比严格等于考评权重"的总数选取与分配（业务口径 2026-09-09）。
 * 总数自动取 [120,200] 内使 总数×权重/权重合计 全为整数的最小可行值。
 */
class StandardStrictTheoryAllocationTest {

  private static ExamStandardNodeEntity section(long id, String weight) {
    ExamStandardNodeEntity e = new ExamStandardNodeEntity();
    e.setId(id);
    e.setWeight(weight == null ? null : new BigDecimal(weight));
    return e;
  }

  private static List<ExamStandardNodeEntity> sections(String... weights) {
    List<ExamStandardNodeEntity> list = new ArrayList<>();
    for (int i = 0; i < weights.length; i++) {
      list.add(section(i + 1L, weights[i]));
    }
    return list;
  }

  /** 含 1%、4% 等整数百分比：120 无法整除，自动放大到 200，占比精确 */
  @Test
  void integerPercentWeights_totalTwoHundredExact() {
    List<ExamStandardNodeEntity> secs = sections(
        "0.01", "0.04", "0.05", "0.10", "0.15", "0.20", "0.20", "0.25");

    StandardLlmSummarizeService.StrictTheoryPlan plan =
        StandardLlmSummarizeService.strictTheoryPlan(secs);
    assertEquals(200, plan.total());
    assertTrue(plan.exact());

    Map<Long, Integer> targets = StandardLlmSummarizeService.allocateTheoryTargetsStrict(secs);
    assertEquals(2, targets.get(1L), "1% → 200×1% = 2");
    assertEquals(8, targets.get(2L), "4% → 200×4% = 8");
    assertEquals(50, targets.get(8L), "25% → 50");
    assertEquals(200, targets.values().stream().mapToInt(Integer::intValue).sum());
  }

  /** 权重均为 5% 的整数倍：120 已可整除，不放大（最小可行总数） */
  @Test
  void fivePercentGranularity_totalOneTwentyExact() {
    List<ExamStandardNodeEntity> secs = sections("0.05", "0.10", "0.15", "0.20", "0.25", "0.25");

    StandardLlmSummarizeService.StrictTheoryPlan plan =
        StandardLlmSummarizeService.strictTheoryPlan(secs);
    assertEquals(120, plan.total());
    assertTrue(plan.exact());

    Map<Long, Integer> targets = StandardLlmSummarizeService.allocateTheoryTargetsStrict(secs);
    assertEquals(6, targets.get(1L));
    assertEquals(30, targets.get(5L));
    assertEquals(120, targets.values().stream().mapToInt(Integer::intValue).sum());
  }

  /** 权重合计 97%（非 100%）：按归一化处理，仍可精确（33:33:31 → 194 点 66:66:62） */
  @Test
  void nonHundredSumNormalizedExact() {
    List<ExamStandardNodeEntity> secs = sections("0.33", "0.33", "0.31");

    StandardLlmSummarizeService.StrictTheoryPlan plan =
        StandardLlmSummarizeService.strictTheoryPlan(secs);
    assertEquals(194, plan.total());
    assertTrue(plan.exact());

    Map<Long, Integer> targets = StandardLlmSummarizeService.allocateTheoryTargetsStrict(secs);
    assertEquals(66, targets.get(1L));
    assertEquals(62, targets.get(3L));
    assertEquals(194, targets.values().stream().mapToInt(Integer::intValue).sum());
  }

  /** 权重粒度太细（0.1%）：精确总数超出上限 200 → 取 200 近似并标记 exact=false */
  @Test
  void tooFineWeights_cappedApproximation() {
    List<ExamStandardNodeEntity> secs = sections("0.001", "0.999");

    StandardLlmSummarizeService.StrictTheoryPlan plan =
        StandardLlmSummarizeService.strictTheoryPlan(secs);
    assertEquals(200, plan.total());
    assertFalse(plan.exact());

    Map<Long, Integer> targets = StandardLlmSummarizeService.allocateTheoryTargetsStrict(secs);
    assertEquals(2, targets.size());
    assertTrue(targets.values().stream().allMatch(v -> v >= 1), "每小节保底 1 点");
  }

  /** 存在无权重小节 → 不适用，返回 null 由调用方回退既有分配 */
  @Test
  void missingWeightInapplicable() {
    List<ExamStandardNodeEntity> secs = sections("0.60", "0.40");
    secs.add(section(9L, null));
    assertNull(StandardLlmSummarizeService.strictTheoryPlan(secs));
    assertNull(StandardLlmSummarizeService.allocateTheoryTargetsStrict(secs));
  }

  @Test
  void emptySectionsInapplicable() {
    assertNull(StandardLlmSummarizeService.strictTheoryPlan(List.of()));
    assertNull(StandardLlmSummarizeService.allocateTheoryTargetsStrict(List.of()));
  }
}
