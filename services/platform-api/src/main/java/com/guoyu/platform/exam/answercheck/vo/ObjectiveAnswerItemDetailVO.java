package com.guoyu.platform.exam.answercheck.vo;

import com.guoyu.platform.exam.support.QuestionContentQualityWarning;
import java.math.BigDecimal;
import java.util.List;

/**
 * 详情抽屉（方案 §5.6）：原解析仅供人工查看并明确“未发送给 AI”。
 */
public record ObjectiveAnswerItemDetailVO(
    ObjectiveAnswerItemVO item,
    String sourceLabel,
    String workTypeTitle,
    String courseTitle,
    Integer levelCode,
    String knowledgePath,
    String contentSnapshot,
    String stemText,
    String resolveSnapshot,
    List<OptionLine> options,
    List<CallLog> calls,
    boolean sourceChanged,
    String modelId,
    String promptVersion,
    String requestedModel,
    String responseModel,
    String confidenceDefinition,
    String failReason) {

  public record OptionLine(
      String optionId,
      String label,
      String text,
      boolean originalCorrect,
      Boolean aiCorrect,
      String choice,
      Double confidence,
      Double referenceProbability,
      Double top1Probability,
      Double top2Probability,
      Double margin) {
  }

  public record CallLog(
      String callId,
      int chunkIndex,
      int chunkCount,
      String status,
      String requestedModel,
      String responseModel,
      String confidenceDefinition,
      Long inputTokens,
      Long outputTokens,
      boolean usageEstimated,
      Integer durationMs,
      String errorCode,
      String errorMessage) {
  }

  /** 供导出复用：内容警告与 needsReview 已由 item 携带。 */
  public record ContentQuality(String quality, List<QuestionContentQualityWarning> warnings,
      BigDecimal referenceConfidence) {
  }
}
