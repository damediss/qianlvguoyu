package com.guoyu.platform.exam.answercheck.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.guoyu.platform.exam.support.QuestionContentQualityWarning;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结果明细（方案 §13.3）。contentQuality 与 resultCode 分开，suspect 不得藏进置信度提示。
 */
public record ObjectiveAnswerItemVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long batchId,
    Integer detailId,
    String questionType,
    String questionTypeLabel,
    String stemPreview,
    String originalAnswer,
    String aiAnswer,
    String resultCode,
    String contentQuality,
    List<QuestionContentQualityWarning> contentWarnings,
    boolean needsReview,
    Boolean consistent,
    BigDecimal referenceConfidence,
    BigDecimal aiConfidence,
    BigDecimal minMargin,
    String decisionWarning,
    boolean retryable,
    int attempts,
    int version,
    String reviewStatus,
    String reviewNote,
    LocalDateTime checkedAt,
    LocalDateTime updatedAt) {
}
