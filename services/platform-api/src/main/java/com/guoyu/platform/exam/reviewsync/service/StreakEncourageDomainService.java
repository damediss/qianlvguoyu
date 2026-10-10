package com.guoyu.platform.exam.reviewsync.service;

import java.util.List;

/**
 * 连对/连错判定与鼓励档位（D9，方案 §15.3）。纯逻辑可单测。
 *
 * <p>阈值（需求方拍板）：连对 3/5/10/15/20 五档、连错 2/3 两档；
 * 连错超过 3 不再升档（第 4 题起学员通常已自发看解析，再给显得敷衍）。
 */
public final class StreakEncourageDomainService {

  private StreakEncourageDomainService() {
  }

  /** 连续同向作答记录：type=correct|wrong，count=连续次数 */
  public record Streak(String type, int count) {
  }

  public static final String TYPE_CORRECT = "correct";
  public static final String TYPE_WRONG = "wrong";

  /** 连对档位 */
  public static final List<Integer> CORRECT_TIERS = List.of(3, 5, 10, 15, 20);
  /** 连错档位 */
  public static final List<Integer> WRONG_TIERS = List.of(2, 3);

  /**
   * 从当日作答流水（按时间序，含改错重答）末尾向前数连续同向作答。
   *
   * @param isCorrectFlags 当日作答对错序列（1=对，0=错），按作答时间升序
   * @return 连续同向记录；序列为空时 count=0
   */
  public static Streak computeStreak(List<Integer> isCorrectFlags) {
    if (isCorrectFlags == null || isCorrectFlags.isEmpty()) {
      return new Streak(TYPE_CORRECT, 0);
    }
    int last = isCorrectFlags.get(isCorrectFlags.size() - 1);
    String type = last == 1 ? TYPE_CORRECT : TYPE_WRONG;
    int count = 0;
    for (int i = isCorrectFlags.size() - 1; i >= 0; i--) {
      if (isCorrectFlags.get(i) == last) {
        count++;
      } else {
        break;
      }
    }
    return new Streak(type, count);
  }

  /**
   * 连续记录对应的鼓励档位：命中档位返回档位值，未命中返回 0。
   * 连对取不超过 count 的最大档位（如连对 7 → 档 5，档 10 在真正到达时触发）；
   * 连错 count>3 封顶档 3（不升档，但 3 档已在 count=3 时触发过，>3 不再触发，见下）。
   */
  public static int tierOf(Streak streak) {
    if (streak == null || streak.count() <= 0) {
      return 0;
    }
    if (TYPE_CORRECT.equals(streak.type())) {
      int tier = 0;
      for (int t : CORRECT_TIERS) {
        if (streak.count() >= t) {
          tier = t;
        }
      }
      return tier;
    }
    if (streak.count() >= 3) {
      return 3;
    }
    return streak.count() >= 2 ? 2 : 0;
  }

  /**
   * 是否应在本次作答触发该档位鼓励：仅当 count 恰好等于档位值时触发
   * （连对 6 时档 5 已发过，不重发；连错 4 时档 3 已发过，不重发）。
   */
  public static boolean shouldFire(Streak streak, int tier) {
    return tier > 0 && streak != null && streak.count() == tier;
  }
}
