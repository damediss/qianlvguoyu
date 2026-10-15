package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** §10.1 调度算法：阶段切分、考试日解析、每日配额、双轨构成（含 183 式极端偏差）、中断 ABCD */
class PlanScheduleDomainServiceTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 4);

  @Test
  void splitStagesForTwentyDays() {
    assertThat(PlanScheduleDomainService.splitStages(20)).containsExactly(10, 6, 4);
    assertThat(PlanScheduleDomainService.splitStages(30)).containsExactly(15, 9, 6);
    assertThat(PlanScheduleDomainService.splitStages(7)).containsExactly(4, 2, 1);
  }

  @Test
  @DisplayName("W2.5 边界：D=3/4/7 三段和=D 且每段≥1（F6 修正，原 D=3 溢出为 4）")
  void splitStagesBoundarySumsToTotal() {
    for (int d : new int[]{3, 4, 7, 20}) {
      int[] stages = PlanScheduleDomainService.splitStages(d);
      assertThat(stages[0] + stages[1] + stages[2]).as("D=%d 三段和", d).isEqualTo(d);
      for (int seg : stages) {
        assertThat(seg).as("D=%d 每段≥1", d).isGreaterThanOrEqualTo(1);
      }
    }
    assertThat(PlanScheduleDomainService.splitStages(3)).containsExactly(1, 1, 1);
    assertThat(PlanScheduleDomainService.splitStages(4)).containsExactly(2, 1, 1);
  }

  @Test
  void resolveExamDatePrefersFutureSingleDate() {
    assertThat(PlanScheduleDomainService.resolveExamDate("2026-08-20", null, TODAY))
        .isEqualTo(LocalDate.of(2026, 8, 20));
    // 单日期已过 → 从列表取最近未来日
    assertThat(PlanScheduleDomainService.resolveExamDate("2026-08-01",
        List.of("2026-08-01", "2026-08-10", "2026-08-25"), TODAY))
        .isEqualTo(LocalDate.of(2026, 8, 10));
    // 全部无效 → null（调用方按默认天数）
    assertThat(PlanScheduleDomainService.resolveExamDate("2026-08-01", List.of("2026-08-02"), TODAY))
        .isNull();
  }

  @Test
  void computeTotalDaysHasThreeDayFloorAndDefault() {
    assertThat(PlanScheduleDomainService.computeTotalDays(LocalDate.of(2026, 8, 24), TODAY, 20)).isEqualTo(20);
    assertThat(PlanScheduleDomainService.computeTotalDays(LocalDate.of(2026, 8, 5), TODAY, 20)).isEqualTo(3);
    assertThat(PlanScheduleDomainService.computeTotalDays(null, TODAY, 20)).isEqualTo(20);
  }

  @Test
  void stage1NewTargetIsDynamicAndCapped() {
    // 1200 题 / 10 天 = 120/天（基准）
    assertThat(PlanScheduleDomainService.stage1NewTarget(1200, 10, 120)).isEqualTo(120);
    // 300 题 / 10 天 = 30/天（稀疏）
    assertThat(PlanScheduleDomainService.stage1NewTarget(300, 10, 120)).isEqualTo(30);
    // 3000 题 / 10 天 = 300 → 封顶 120
    assertThat(PlanScheduleDomainService.stage1NewTarget(3000, 10, 120)).isEqualTo(120);
    // 最后一天剩余全部
    assertThat(PlanScheduleDomainService.stage1NewTarget(300, 1, 120)).isEqualTo(120);
    assertThat(PlanScheduleDomainService.stage1NewTarget(0, 5, 120)).isZero();
  }

  @Test
  void totalCapLetsReviewPreemptNew() {
    assertThat(PlanScheduleDomainService.applyTotalCap(100, 50, 200)).containsExactly(100, 50);
    assertThat(PlanScheduleDomainService.applyTotalCap(150, 100, 200)).containsExactly(100, 100);
    assertThat(PlanScheduleDomainService.applyTotalCap(150, 250, 200)).containsExactly(0, 250);
  }

  @Test
  void stage1CompositionDualTrackWithExtremeSkew() {
    // 183 式极端：职业道德剩余 1077 题（权重 5%），生产操作 456（40%），基础知识 412（35%）
    Map<Long, Integer> remaining = new LinkedHashMap<>();
    remaining.put(11L, 1077); // 职业道德
    remaining.put(12L, 412);  // 基础知识
    remaining.put(13L, 456);  // 生产操作
    Map<Long, Double> weights = Map.of(11L, 0.05, 12L, 0.35, 13L, 0.40);

    // 第 1 天（剩余 10 天，目标 120）：保底配额 = ceil(剩余/10) → 108+42+46=196 > 120 → 收敛
    Map<Long, Integer> day1 = PlanScheduleDomainService.stage1Composition(remaining, weights, 120, 10);
    int day1Total = day1.values().stream().mapToInt(Integer::intValue).sum();
    assertThat(day1Total).isEqualTo(120);
    // 保底合计(196)超目标(120)：按保底比例压缩——职业道德仍拿到压缩后的大头（远多于权重理想的 6 题）
    assertThat(day1.get(11L)).isGreaterThan(6);
    assertThat(day1.get(11L)).isLessThan(108);
    // 生产操作拿到压缩保底份额（46×120/196≈28）+ 权重补零头
    assertThat(day1.get(13L)).isGreaterThanOrEqualTo(28);

    // 最后一天（剩余 1 天）：保底 = 全部剩余 → 必做池必然清空（覆盖硬约束）
    Map<Long, Integer> lastDay = PlanScheduleDomainService.stage1Composition(remaining, weights, 3000, 1);
    assertThat(lastDay.get(11L)).isEqualTo(1077);
    assertThat(lastDay.get(12L)).isEqualTo(412);
    assertThat(lastDay.get(13L)).isEqualTo(456);
  }

  @Test
  void stage1CompositionWithoutWeightsCoversFairly() {
    Map<Long, Integer> remaining = Map.of(1L, 60, 2L, 40);
    Map<Long, Integer> composition = PlanScheduleDomainService.stage1Composition(
        remaining, Map.of(), 100, 1);
    assertThat(composition.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(100);
  }

  @Test
  void interruptDetectionMapsStagesToCases() {
    LocalDate lastActive = TODAY.minusDays(3);
    assertThat(PlanScheduleDomainService.detectInterruptCase(1, lastActive, TODAY, 2, 0))
        .isEqualTo(PlanScheduleDomainService.InterruptCase.A);
    assertThat(PlanScheduleDomainService.detectInterruptCase(2, lastActive, TODAY, 2, 0))
        .isEqualTo(PlanScheduleDomainService.InterruptCase.B);
    assertThat(PlanScheduleDomainService.detectInterruptCase(3, lastActive, TODAY, 2, 0))
        .isEqualTo(PlanScheduleDomainService.InterruptCase.C);
    // 反复中断 → D
    assertThat(PlanScheduleDomainService.detectInterruptCase(1, lastActive, TODAY, 2, 2))
        .isEqualTo(PlanScheduleDomainService.InterruptCase.D);
    // gap 不足 → 不中断
    assertThat(PlanScheduleDomainService.detectInterruptCase(1, TODAY.minusDays(1), TODAY, 2, 0))
        .isNull();
    assertThat(PlanScheduleDomainService.detectInterruptCase(1, null, TODAY, 2, 0)).isNull();
  }

  @Test
  void caseATailTrimOnlyWhenDailyExceeds180() {
    // 断 4 天：剩 6 天要刷 1200 → 200/天 > 180 → 触发
    assertThat(PlanScheduleDomainService.caseATailTrimCount(1200, 6, 1200))
        .isEqualTo(Math.min(1200 - 180 * 6, (int) Math.ceil(1200 * 0.15)));
    // 剩 10 天刷 1200 → 120/天 ≤ 180 → 不触发
    assertThat(PlanScheduleDomainService.caseATailTrimCount(1200, 10, 1200)).isZero();
    // 删除量不超过 target × 15%
    assertThat(PlanScheduleDomainService.caseATailTrimCount(5000, 3, 2000))
        .isLessThanOrEqualTo((int) Math.ceil(2000 * 0.15));
  }
}
