package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_make_exam_paper_question")
public class CompetitionMakeExamPaperQuestionEntity extends BaseEntity {

  private Long paperSnapshotId;
  private Long sourceQuestionId;
  private Integer sortOrder;
  private String questionType;
  private String stem;
  private String optionsJson;
  private String referenceAnswer;
  private String fillAnswer;
  private BigDecimal score;
  private Integer difficulty;
  private String operationContent;
  private String studentPrepare;
  private String equipmentPrepare;
  private String scoringItemsJson;

  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getSourceQuestionId() { return sourceQuestionId; }
  public void setSourceQuestionId(Long sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public String getStem() { return stem; }
  public void setStem(String stem) { this.stem = stem; }
  public String getOptionsJson() { return optionsJson; }
  public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
  public String getReferenceAnswer() { return referenceAnswer; }
  public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
  public String getFillAnswer() { return fillAnswer; }
  public void setFillAnswer(String fillAnswer) { this.fillAnswer = fillAnswer; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public String getOperationContent() { return operationContent; }
  public void setOperationContent(String operationContent) { this.operationContent = operationContent; }
  public String getStudentPrepare() { return studentPrepare; }
  public void setStudentPrepare(String studentPrepare) { this.studentPrepare = studentPrepare; }
  public String getEquipmentPrepare() { return equipmentPrepare; }
  public void setEquipmentPrepare(String equipmentPrepare) { this.equipmentPrepare = equipmentPrepare; }
  public String getScoringItemsJson() { return scoringItemsJson; }
  public void setScoringItemsJson(String scoringItemsJson) { this.scoringItemsJson = scoringItemsJson; }
}
