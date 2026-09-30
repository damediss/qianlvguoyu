package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运营数据面板「同步练习」分区 VO（《运营数据面板_同步练习统计面板方案》v0.1 §5）。
 * 比率字段为百分数（0-100，两位小数）；分母为 0 时返回 null，前端显示"--"。
 */
public record ReviewSyncPanelVO(
    Scale scale,
    Effort effort,
    Effect effect,
    List<TrendPoint> trend30d,
    List<WrongTrendPoint> wrongTrend30d,
    List<WeakItem> weakTop5) {

  /** 规模（用没用）：activeRate7d 分母为进行中计划数 */
  public record Scale(long totalPlans, long active, long completed, long abandoned,
      BigDecimal abandonRate, long active7d, BigDecimal activeRate7d, long interruptPlans) {
  }

  /** 投入（勤不勤）：totalAnswers 为累计口径，dayCompletionRate 为近 N 天口径 */
  public record Effort(long totalAnswers, BigDecimal avgAnswersPerPlan,
      BigDecimal dayCompletionRate, long todayActive, long todayAnswers) {
  }

  /** 效果（好不好）：correctRate 为近 N 天口径 */
  public record Effect(BigDecimal correctRate, long removedWrongs, long redWrongs, long yellowWrongs,
      long mockDone, BigDecimal mockAvgScore, BigDecimal mockPassRate) {
  }

  /** 每日趋势点：correctRate 为 null 表示当日无作答 */
  public record TrendPoint(String date, long answers, long activeUsers, BigDecimal correctRate) {
  }

  /** 每日错题新增/消灭 */
  public record WrongTrendPoint(String date, long newCnt, long removedCnt) {
  }

  /** 薄弱考核项目（L1，近 N 天作答 ≥50 才参评） */
  public record WeakItem(Long knowledgeId, String title, long answers, BigDecimal correctRate) {
  }
}
