package com.guoyu.platform.competition.exam.formal.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FormalExamScoringDecimals {

  public static final int SCALE = 2;
  public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
  public static final int CURRENT_RULE_VERSION = 1;

  private FormalExamScoringDecimals() {
  }

  public static BigDecimal money(BigDecimal value) {
    if (value == null) {
      throw new FormalExamScoringException("题目分值不能为空");
    }
    return value.setScale(SCALE, ROUNDING);
  }

  public static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
  }
}
