package com.guoyu.platform.ai.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SystemOne 单个子问题（方案 §9.3）。
 *
 * @param instructions 该子问题的作答指令
 * @param criteria     候选 key → 说明；必须 ≥2 项且 ≤255 项
 */
public record JevQuestion(String instructions, Map<String, String> criteria) {

  public JevQuestion {
    criteria = criteria == null ? new LinkedHashMap<>() : new LinkedHashMap<>(criteria);
  }
}
