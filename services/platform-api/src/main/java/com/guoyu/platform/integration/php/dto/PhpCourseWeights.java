package com.guoyu.platform.integration.php.dto;

import java.util.List;

/**
 * 课程权重配置（exam_knowledge_rate + detail，挂理论两树 L1 节点）。
 *
 * @param sourceType standard=type=3 国标 / custom=type=0 人工 / null=无配置（调用方走题量兜底）
 * @param items     权重项（L1 knowledgeId + 标题 + rate%）
 */
public record PhpCourseWeights(String sourceType, List<WeightItem> items) {

  public record WeightItem(long knowledgeId, String title, int rate) {
  }

  public boolean isEmpty() {
    return items == null || items.isEmpty();
  }
}
