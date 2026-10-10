package com.guoyu.platform.exam.standard.dto;

import java.util.List;

/**
 * AI 概括触发请求（方案 §11 #3 扩展）：
 * scopes = 选中的「等级×工种」组合（null/缺省 = 任务全部有效组合）；
 * importants = 理论通道重要程度档位（X/Y/Z 子集，缺省 = XYZ 全档）。
 */
public record StandardSummarizeReq(List<Scope> scopes, List<String> importants) {

  /** 等级×工种组合 */
  public record Scope(Integer levelCode, String direction) {
  }
}
