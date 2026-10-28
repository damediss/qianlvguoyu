package com.guoyu.platform.exam.answercheck.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 客观题答案 AI 检查明细快照（exam_objective_answer_check_item，方案 §12.3）。 */
@Getter
@Setter
@TableName("exam_objective_answer_check_item")
public class ObjectiveAnswerCheckItemEntity extends BaseEntity {

  private Long batchId;
  private Integer detailId;
  private String questionType;
  private Integer source;
  private Integer workTypeId;
  private String workTypeTitle;
  private Integer levelCode;
  private Integer courseId;
  private String courseTitle;
  private Integer knowledgeId;
  private String knowledgePath;
  private String contentSnapshot;
  private String stemText;
  private String answerSnapshot;
  private String resolveSnapshot;
  private String optionSnapshotJson;
  private String sourceHash;
  private String contentQuality;
  private String contentWarningsJson;
  private String processStatus;
  private String resultCode;
  private Integer consistent;
  private String originalAnswerKeys;
  private String aiAnswerKeys;
  private BigDecimal referenceConfidence;
  private BigDecimal aiConfidence;
  private BigDecimal minMargin;
  private String optionResultsJson;
  private String decisionWarning;
  private String invalidReason;
  private String errorCode;
  private String errorMessage;
  private Integer retryable;
  private Integer attempts;
  private Integer version;
  private String reviewStatus;
  private String reviewNote;
  private Long reviewedBy;
  private LocalDateTime reviewedAt;
  private LocalDateTime checkedAt;
}
