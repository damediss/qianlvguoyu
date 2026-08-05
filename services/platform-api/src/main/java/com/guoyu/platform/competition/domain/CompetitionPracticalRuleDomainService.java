package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.dto.CompetitionPracticalRuleItemReq;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;

public final class CompetitionPracticalRuleDomainService {
  public BigDecimal validateAndTotal(List<CompetitionPracticalRuleItemReq> items) {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("实操规则至少需要一个评分项");
    }
    HashSet<String> codes = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO;
    for (CompetitionPracticalRuleItemReq item : items) {
      if (item == null || item.itemCode() == null || item.itemCode().isBlank()
          || !codes.add(item.itemCode())) {
        throw new IllegalArgumentException("实操评分项编码不能为空且不能重复");
      }
      if (item.maxScore() == null || item.maxScore().signum() <= 0) {
        throw new IllegalArgumentException("实操评分项分值必须大于0");
      }
      total = total.add(item.maxScore());
    }
    if (total.compareTo(new BigDecimal("100.00")) != 0) {
      throw new IllegalArgumentException("实操评分项分值合计必须为100分");
    }
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  public BigDecimal validateEntry(
      BigDecimal score, BigDecimal deduction, BigDecimal maxScore, boolean deductionAllowed) {
    if (score == null || deduction == null || score.signum() < 0 || deduction.signum() < 0) {
      throw new IllegalArgumentException("评分和扣分不能为负数");
    }
    if (score.compareTo(maxScore) > 0 || (!deductionAllowed && deduction.signum() > 0)) {
      throw new IllegalArgumentException("评分项超出规则上限或不允许扣分");
    }
    BigDecimal value = score.subtract(deduction);
    if (value.signum() < 0) {
      throw new IllegalArgumentException("评分项扣分不能超过得分");
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
