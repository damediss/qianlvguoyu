package com.guoyu.platform.exam.reviewsync.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 错题三级淘汰状态机（§5.4）。纯逻辑可单测——输入错题当前状态 + 事件，输出新状态。
 *
 * <pre>
 * 作答判错 ──→ 红牌 red（consecutive=0）
 * 红牌复盘对 → consecutive+1；≥3 → 黄牌（清零，记 lastCorrectDate）
 * 红牌复盘错 → consecutive=0、star=1（未来 3 天每日必出）
 * 黄牌复盘对 → consecutive+1、更新 lastCorrectDate；≥2 且 与上次答对间隔>3天 → 绿牌（removed=1）
 * 黄牌复盘错 → 回红牌、清零、star=1（从严，O2）
 * 当日新错题一律红牌、当日不可移除
 * </pre>
 */
public final class WrongCardDomainService {

  private WrongCardDomainService() {
  }

  public static final String RED = "red";
  public static final String YELLOW = "yellow";
  public static final String GREEN = "green";

  /** 红牌降级黄牌所需连续答对次数 */
  public static final int RED_TO_YELLOW_STREAK = 3;
  /** 黄牌移除所需连续答对次数 */
  public static final int YELLOW_TO_GREEN_STREAK = 2;
  /** 黄牌移除所需两次答对的最小间隔天数 */
  public static final int YELLOW_TO_GREEN_MIN_GAP_DAYS = 3;

  /**
   * 错题卡状态（不可变快照，便于纯函数测试）。
   *
   * @param exists              是否已存在（false=首次答错新建）
   * @param cardLevel           red/yellow/green
   * @param star                是否加星
   * @param consecutiveCorrect  连续答对计数
   * @param lastCorrectDate     最近一次答对日期
   * @param removed             是否已移出
   */
  public record CardState(boolean exists, String cardLevel, boolean star, int consecutiveCorrect,
      LocalDate lastCorrectDate, boolean removed) {

    public static CardState none() {
      return new CardState(false, null, false, 0, null, false);
    }
  }

  /** 状态迁移结果（from → to + star 变化，供接口 7.4 返回 cardChange） */
  public record CardTransition(String fromLevel, String toLevel, boolean star, int consecutiveCorrect,
      LocalDate lastCorrectDate, boolean removed) {

    public boolean levelChanged() {
      // 新建（from=null → red）也算变化，供前端展示"已加入红牌错题本"
      return !java.util.Objects.equals(fromLevel, toLevel);
    }
  }

  /** 作答判错 → 红牌（当日新错题一律红牌；黄牌答错回红牌加星（O2 从严）；已红牌再错加星） */
  public static CardTransition onWrong(CardState state, LocalDate today) {
    if (!state.exists() || state.removed()) {
      // 首次答错（或绿牌移除后再错）：新建红牌
      return new CardTransition(state.cardLevel(), RED, false, 0, null, false);
    }
    if (YELLOW.equals(state.cardLevel())) {
      // 黄牌答错 → 回红牌 + 加星（O2）
      return new CardTransition(YELLOW, RED, true, 0, state.lastCorrectDate(), false);
    }
    // 红牌复盘再错 → 加星，计数清零
    return new CardTransition(RED, RED, true, 0, state.lastCorrectDate(), false);
  }

  /** 作答判对 → 按牌级推进；绿牌题答对无变化 */
  public static CardTransition onCorrect(CardState state, LocalDate today) {
    if (!state.exists() || state.removed()) {
      return new CardTransition(state.cardLevel(), state.cardLevel(), state.star(),
          state.consecutiveCorrect(), state.lastCorrectDate(), state.removed());
    }
    if (RED.equals(state.cardLevel())) {
      int streak = state.consecutiveCorrect() + 1;
      if (streak >= RED_TO_YELLOW_STREAK) {
        return new CardTransition(RED, YELLOW, state.star(), 0, today, false);
      }
      return new CardTransition(RED, RED, state.star(), streak, state.lastCorrectDate(), false);
    }
    if (YELLOW.equals(state.cardLevel())) {
      // 与上次答对的间隔（在更新前计算）
      long gapDays = state.lastCorrectDate() == null ? Long.MAX_VALUE
          : ChronoUnit.DAYS.between(state.lastCorrectDate(), today);
      int streak = state.consecutiveCorrect() + 1;
      if (streak >= YELLOW_TO_GREEN_STREAK && gapDays > YELLOW_TO_GREEN_MIN_GAP_DAYS) {
        return new CardTransition(YELLOW, GREEN, false, streak, today, true);
      }
      return new CardTransition(YELLOW, YELLOW, state.star(), streak, today, false);
    }
    return new CardTransition(state.cardLevel(), state.cardLevel(), state.star(),
        state.consecutiveCorrect(), state.lastCorrectDate(), state.removed());
  }

  /**
   * 中断残酷清洗（§5.4）：近 14 天内移除的绿牌 → 回黄牌；全部在册红/黄牌 consecutive 清零。
   *
   * @param state          当前状态
   * @param removedDaysAgo 移除至今的天数（仅绿牌有意义；非绿牌传 -1）
   */
  public static CardTransition onInterruptWash(CardState state, LocalDate today, long removedDaysAgo) {
    if (!state.exists()) {
      return new CardTransition(null, null, false, 0, null, false);
    }
    if (state.removed() && removedDaysAgo >= 0 && removedDaysAgo <= 14) {
      // 绿牌回黄，连续答对清零
      return new CardTransition(GREEN, YELLOW, state.star(), 0, state.lastCorrectDate(), false);
    }
    if (!state.removed()) {
      // 在册红/黄牌计数清零（牌级不动）
      return new CardTransition(state.cardLevel(), state.cardLevel(), state.star(), 0,
          state.lastCorrectDate(), false);
    }
    return new CardTransition(state.cardLevel(), state.cardLevel(), state.star(),
        state.consecutiveCorrect(), state.lastCorrectDate(), state.removed());
  }

  /** 红牌是否"明日必出"（红牌全部每日必出；加星强制置顶——排序由调用方处理） */
  public static boolean isDailyMandatory(CardState state) {
    return state.exists() && !state.removed() && RED.equals(state.cardLevel());
  }
}
