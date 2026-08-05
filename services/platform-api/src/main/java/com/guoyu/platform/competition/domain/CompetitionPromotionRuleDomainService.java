package com.guoyu.platform.competition.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure promotion-rule validation and calculation shared by planning and settlement. */
public final class CompetitionPromotionRuleDomainService {
  public static final String FIXED_COUNT = "FIXED_COUNT";
  public static final String PERCENTAGE = "PERCENTAGE";
  public static final String FINAL_PODIUM = "FINAL_PODIUM";
  public static final String LEGACY = "LEGACY";

  public ResolvedRule resolve(String ruleType, Integer quota, Integer percentage) {
    String resolved = ruleType;
    if (resolved == null || resolved.isBlank()) {
      resolved = percentage != null ? PERCENTAGE : quota != null ? FIXED_COUNT : LEGACY;
    }
    if (!Set.of(FIXED_COUNT, PERCENTAGE, FINAL_PODIUM, LEGACY).contains(resolved)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "晋级规则类型无效");
    }
    if (FIXED_COUNT.equals(resolved) && (quota == null || quota < 0)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "固定人数晋级必须填写有效晋级人数");
    }
    if (PERCENTAGE.equals(resolved) && (percentage == null || percentage < 1 || percentage > 100)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "百分比晋级必须填写1到100的整数");
    }
    if (FINAL_PODIUM.equals(resolved) && (quota != null || percentage != null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "决赛前三名规则不允许填写人数或百分比");
    }
    return new ResolvedRule(resolved, quota, percentage, LEGACY.equals(resolved));
  }

  public int calculateTargetCount(int baseCount, ResolvedRule rule) {
    if (baseCount < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "晋级基数不能为负数");
    }
    return switch (rule.type()) {
      case FIXED_COUNT -> Math.min(baseCount, rule.quota());
      case PERCENTAGE -> (int) Math.ceil(baseCount * rule.percentage() / 100.0d);
      case FINAL_PODIUM, LEGACY -> 0;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "晋级规则类型无效");
    };
  }

  public boolean selectedByRank(Integer rank, int calculatedTargetCount) {
    return rank != null && calculatedTargetCount > 0 && rank <= calculatedTargetCount;
  }

  public void validateFinalPodium(List<Long> podium, int completedCount, String explanation) {
    if (podium == null || podium.size() != 3) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "决赛前三名必须提供三个名次槽位");
    }
    Set<Long> nonNull = new HashSet<>();
    for (Long memberId : podium) {
      if (memberId != null && !nonNull.add(memberId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "决赛第1、2、3名不能重复");
      }
    }
    if (completedCount >= 3 && nonNull.size() != 3) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "有效选手不少于3人时必须确认完整前三名");
    }
    if (completedCount < 3 && (explanation == null || explanation.isBlank())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "有效选手不足3人时必须填写特殊说明");
    }
  }

  public record ResolvedRule(String type, Integer quota, Integer percentage, boolean legacy) { }
}
