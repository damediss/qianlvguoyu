package com.guoyu.platform.exam.knowledgefix.dto;

import java.util.List;

/** 定夺结果：成功数 + 跳过明细（非存疑/并发改走/越界），批量不整批失败 */
public record AdjudicateResultVO(long succeeded, List<SkipVO> skipped) {

  public record SkipVO(long rowId, String reason) {
  }

  public static AdjudicateResultVO of(long succeeded, List<SkipVO> skipped) {
    return new AdjudicateResultVO(succeeded, List.copyOf(skipped));
  }
}
