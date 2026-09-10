package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_question")
public class CompetitionQuestionEntity extends BaseEntity {

  private Long eventId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private String questionType;
  private Integer sortOrder;
  private String stem;
  /** 历史兼容字段；竞赛实际分值由组卷计划明细决定。 */
  @Deprecated
  private BigDecimal score;
  private String referenceAnswer;
  private String fillAnswer;
  private String optionsJson;
  private Integer difficulty;
  private String status;
  private Long sourceBatchId;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getStem() { return stem; }
  public void setStem(String stem) { this.stem = stem; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public String getReferenceAnswer() { return referenceAnswer; }
  public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
  public String getFillAnswer() { return fillAnswer; }
  public void setFillAnswer(String fillAnswer) { this.fillAnswer = fillAnswer; }
  public String getOptionsJson() { return optionsJson; }
  public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getSourceBatchId() { return sourceBatchId; }
  public void setSourceBatchId(Long sourceBatchId) { this.sourceBatchId = sourceBatchId; }
}
