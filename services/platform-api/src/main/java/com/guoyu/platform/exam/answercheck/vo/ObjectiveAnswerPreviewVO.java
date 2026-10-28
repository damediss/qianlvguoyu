package com.guoyu.platform.exam.answercheck.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预览返回（方案 §7.4）。
 *
 * <p>invalidCount 按题去重（结构预检 invalid ∪ contentQuality=invalid）；
 * invalidContentCount 是其中内容质量子集，二者不能相加。
 */
public record ObjectiveAnswerPreviewVO(
    int selectionCount,
    boolean overLimit,
    int maxBatchSize,
    Integer validCount,
    Integer invalidCount,
    Integer suspectContentCount,
    Integer invalidContentCount,
    Integer expectedCalls,
    Map<String, Integer> typeCounts,
    List<InvalidReasonCount> invalidReasonCounts,
    String modelId,
    BigDecimal confidenceThreshold,
    BigDecimal marginThreshold) {

  public record InvalidReasonCount(String code, int count) {
  }

  public static ObjectiveAnswerPreviewVO overLimit(int count, int maxBatchSize, String modelId) {
    return new ObjectiveAnswerPreviewVO(count, true, maxBatchSize, null, null, null, null, null,
        null, List.of(), modelId, null, null);
  }
}
