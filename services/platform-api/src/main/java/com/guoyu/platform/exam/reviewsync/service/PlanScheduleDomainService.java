package com.guoyu.platform.exam.reviewsync.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 三阶段调度与中断补救决策（§5.1 / §5.2 / §5.5）。纯逻辑可单测。
 */
public final class PlanScheduleDomainService {

  private PlanScheduleDomainService() {
  }

  /** 中断判定类型 */
  public enum InterruptCase {A, B, C, D}

  // ------------------------------------------------------------------
  // §5.1 计划生成
  // ------------------------------------------------------------------

  /**
   * 阶段切分：D1=round(D×0.5)、D2=round(D×0.3)、D3=尾差。
   * 边界修正（F6）：保证 d1+d2+d3=D 且 D≥3 时每段 ≥1（原 D=3 时修正后三段和溢出为 4）。
   */
  public static int[] splitStages(int totalDays) {
    int d1 = (int) Math.round(totalDays * 0.5);
    int d2 = (int) Math.round(totalDays * 0.3);
    int d3 = totalDays - d1 - d2;
    if (d3 < 1) {
      d3 = 1;
      d2 = totalDays - d1 - d3;
      if (d2 < 1) {
        d2 = 1;
        d1 = totalDays - d2 - d3;
      }
    }
    return new int[]{d1, d2, d3};
  }

  /** 考试日解析：exam_date（yyyy-MM-dd）→ 未来有效；否则 exam_date_list（JSON 日期数组取最近未来日） */
  public static LocalDate resolveExamDate(String examDate, List<String> examDateList, LocalDate today) {
    LocalDate parsed = parseDate(examDate);
    if (parsed != null && !parsed.isBefore(today)) {
      return parsed;
    }
    LocalDate nearest = null;
    if (examDateList != null) {
      for (String raw : examDateList) {
        LocalDate candidate = parseDate(raw);
        if (candidate != null && !candidate.isBefore(today)
            && (nearest == null || candidate.isBefore(nearest))) {
          nearest = candidate;
        }
      }
    }
    return nearest;
  }

  /** 总天数：考试日 - 今天（下限 3 天）；无考试日 → 默认天数 */
  public static int computeTotalDays(LocalDate examDate, LocalDate today, int defaultDays) {
    if (examDate == null) {
      return defaultDays;
    }
    long days = ChronoUnit.DAYS.between(today, examDate);
    return (int) Math.max(3, days);
  }

  private static LocalDate parseDate(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.length() >= 10) {
      trimmed = trimmed.substring(0, 10);
    }
    try {
      return LocalDate.parse(trimmed);
    } catch (Exception e) {
      return null;
    }
  }

  // ------------------------------------------------------------------
  // §5.2 每日配额
  // ------------------------------------------------------------------

  /** 阶段 1 新题目标：min(dailyNewCap, ceil(remainingNew / 剩余阶段1天数)) */
  public static int stage1NewTarget(int remainingNew, int remainingStage1Days, int dailyNewCap) {
    if (remainingNew <= 0) {
      return 0;
    }
    int days = Math.max(1, remainingStage1Days);
    return Math.min(dailyNewCap, (int) Math.ceil(remainingNew / (double) days));
  }

  /** 总量封顶：new + review > dailyTotalCap 时新题让位（复盘优先） */
  public static int[] applyTotalCap(int newTarget, int reviewTarget, int dailyTotalCap) {
    if (newTarget + reviewTarget <= dailyTotalCap) {
      return new int[]{newTarget, reviewTarget};
    }
    return new int[]{Math.max(0, dailyTotalCap - reviewTarget), reviewTarget};
  }

  /**
   * 阶段 1 新题的考核项目构成（D6 双轨）：
   * 每日各项目配额 = max(权重理想配额, 完成保底配额)，再按 newTarget 收敛。
   *
   * @param remainingByL1 各 L1 项目在必做池中的剩余题数（key=L1 id）
   * @param weightByL1    各 L1 归一化权重（Σ=1）
   * @param newTarget     当日新题目标量
   * @param remainingDays 阶段1剩余天数（含今天）
   * @return 各 L1 当日应出题数（Σ = newTarget 或剩余总量）
   */
  public static Map<Long, Integer> stage1Composition(Map<Long, Integer> remainingByL1,
      Map<Long, Double> weightByL1, int newTarget, int remainingDays) {
    Map<Long, Integer> composition = new LinkedHashMap<>();
    if (newTarget <= 0 || remainingByL1.isEmpty()) {
      return composition;
    }
    int totalRemaining = remainingByL1.values().stream().mapToInt(Integer::intValue).sum();
    int target = Math.min(newTarget, totalRemaining);
    int days = Math.max(1, remainingDays);

    // 双轨取大：理想（权重×target）vs 保底（ceil(项目剩余/剩余天数)）
    Map<Long, Integer> ideal = new LinkedHashMap<>();
    Map<Long, Integer> guarantee = new LinkedHashMap<>();
    int guaranteeSum = 0;
    for (Map.Entry<Long, Integer> entry : remainingByL1.entrySet()) {
      long l1 = entry.getKey();
      int remain = entry.getValue();
      int idealQuota = (int) Math.round(weightByL1.getOrDefault(l1, 0.0) * target);
      int guaranteeQuota = (int) Math.ceil(remain / (double) days);
      ideal.put(l1, Math.min(idealQuota, remain));
      int cappedGuarantee = Math.min(guaranteeQuota, remain);
      guarantee.put(l1, cappedGuarantee);
      guaranteeSum += cappedGuarantee;
    }
    List<Long> byWeight = new ArrayList<>(remainingByL1.keySet());
    byWeight.sort((a, b) -> Double.compare(weightByL1.getOrDefault(b, 0.0), weightByL1.getOrDefault(a, 0.0)));

    int assigned;
    if (guaranteeSum > target) {
      // 保底合计超目标：按保底比例压缩到 target（保底优先、向下取整后按权重顺序补零头）
      assigned = 0;
      for (Long l1 : byWeight) {
        int scaled = (int) Math.floor(guarantee.get(l1) * (double) target / guaranteeSum);
        composition.put(l1, scaled);
        assigned += scaled;
      }
      int leftover = target - assigned;
      for (Long l1 : byWeight) {
        if (leftover <= 0) {
          break;
        }
        int current = composition.get(l1);
        int capacity = Math.min(guarantee.get(l1), remainingByL1.get(l1));
        if (current < capacity) {
          composition.put(l1, current + 1);
          leftover--;
          assigned++;
        }
      }
    } else {
      // 保底线先铺，再用理想权重补满 target（保底优先、理想按权重比例压缩）
      assigned = 0;
      for (Map.Entry<Long, Integer> entry : remainingByL1.entrySet()) {
        composition.put(entry.getKey(), guarantee.get(entry.getKey()));
        assigned += guarantee.get(entry.getKey());
      }
      int guard = 0;
      while (assigned < target && guard++ < target * 4 + 16) {
        boolean progressed = false;
        for (Long l1 : byWeight) {
          int current = composition.get(l1);
          int remain = remainingByL1.get(l1);
          int idealTarget = Math.max(ideal.get(l1), guarantee.get(l1));
          if (assigned < target && current < remain && current < idealTarget) {
            composition.put(l1, current + 1);
            assigned++;
            progressed = true;
          }
        }
        if (!progressed) {
          // 理想配额已满但 target 未满（权重偏斜）：按剩余量兜底填满（完成优先）
          for (Long l1 : byWeight) {
            int current = composition.get(l1);
            if (assigned < target && current < remainingByL1.get(l1)) {
              composition.put(l1, current + 1);
              assigned++;
              progressed = true;
            }
          }
          if (!progressed) {
            break;
          }
        }
      }
    }
    composition.values().removeIf(v -> v <= 0);
    return composition;
  }

  // ------------------------------------------------------------------
  // §5.5 中断检测与 ABCD 决策
  // ------------------------------------------------------------------

  /**
   * 中断检测：gap = today - lastActiveDate，gap ≥ interruptGap 触发。
   * D（零星中断）：interruptCount ≥ 2（近 7 天内反复中断的近似判定，见 §5.5 注）。
   *
   * @return A/B/C/D；null=未中断
   */
  public static InterruptCase detectInterruptCase(int currentStage, LocalDate lastActiveDate,
      LocalDate today, int interruptGapDays, int interruptCount) {
    if (lastActiveDate == null) {
      return null;
    }
    long gap = ChronoUnit.DAYS.between(lastActiveDate, today);
    if (gap < interruptGapDays) {
      return null;
    }
    if (interruptCount >= 2) {
      return InterruptCase.D;
    }
    return switch (currentStage) {
      case 1 -> InterruptCase.A;
      case 2 -> InterruptCase.B;
      default -> InterruptCase.C;
    };
  }

  /** 情况 A 恢复第 1 天：预热 80 新题且仅单选+判断 */
  public static final int CASE_A_PREHEAT_NEW = 80;
  /** 情况 A 第 2 天起每日新题上限 180，超出则删尾部偏题 */
  public static final int CASE_A_DAILY_NEW_HARD_CAP = 180;
  /** 情况 A 尾部偏题删除比例上限（target × 15%） */
  public static final double CASE_A_TRIM_RATIO = 0.15;

  /** 情况 A 第 2 天起新题目标：剩余必做 ÷ 剩余阶段1天数 */
  public static int caseANewTarget(int remainingNew, int remainingStage1Days) {
    if (remainingNew <= 0) {
      return 0;
    }
    return (int) Math.ceil(remainingNew / (double) Math.max(1, remainingStage1Days));
  }

  /**
   * 情况 A 是否触发尾部删除：每日新题 > 180。
   *
   * @return 需要删除的题数（= remainingNew - 180×剩余天数，且 ≤ ceil(target×15%)）；0=无需删除
   */
  public static int caseATailTrimCount(int remainingNew, int remainingStage1Days, int targetQuestions) {
    int days = Math.max(1, remainingStage1Days);
    if (caseANewTarget(remainingNew, days) <= CASE_A_DAILY_NEW_HARD_CAP) {
      return 0;
    }
    int excess = remainingNew - CASE_A_DAILY_NEW_HARD_CAP * days;
    int maxTrim = (int) Math.ceil(targetQuestions * CASE_A_TRIM_RATIO);
    return Math.max(0, Math.min(excess, maxTrim));
  }

  /** 情况 B 摸底测验题量（从未做过的混编题） */
  public static final int CASE_B_QUIZ_SIZE = 100;
  /** 情况 B 摸底通过线（正确率 ≥ 70% 提前进阶段 3） */
  public static final double CASE_B_PASS_ACCURACY = 0.7;
  /** 情况 D 热车题量（前一次任务最后 50 题） */
  public static final int CASE_D_WARMUP_SIZE = 50;
  /** 情况 D 每日新题固定下调 */
  public static final int CASE_D_DAILY_NEW = 100;

  /** 阶段 2 新题上限（薄弱知识点剩余题） */
  public static final int STAGE2_NEW_CAP = 50;
  /** 阶段 2 复盘上限 */
  public static final int STAGE2_REVIEW_CAP = 300;
}
