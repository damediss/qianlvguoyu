package com.guoyu.platform.textbook.vo;

import java.util.List;

/** 确定性检查报告（方案 §8.3）：非 LLM，仅供提示，不阻断提交（空正文/final 占位除外）。 */
public record TextbookCheckReportVO(
    Integer wordCount,
    Integer targetWords,
    Integer minSuggestedWords,
    Integer maxSuggestedWords,
    Boolean withinTargetRange,
    List<UncoveredPoint> uncoveredPoints,
    Integer placeholderCount,
    String checkedAt) {

  /** 可能未覆盖的标准点：完整标题包含匹配未命中，仅提示。 */
  public record UncoveredPoint(String pointId, String title, String dimension) {
  }
}
