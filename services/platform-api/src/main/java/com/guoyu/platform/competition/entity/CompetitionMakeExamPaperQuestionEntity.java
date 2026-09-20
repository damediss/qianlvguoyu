package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_make_exam_paper_question")
public class CompetitionMakeExamPaperQuestionEntity extends BaseEntity {

  private Long paperSnapshotId;
  private Long sourceQuestionId;
  private Long blueprintId;
  private Long blueprintNodeId;
  private String blueprintFullCode;
  private String blueprintPath;
  private Integer sortOrder;
  private String questionType;
  private String stem;
  private String optionsJson;
  private String referenceAnswer;
  private String fillAnswer;
  private BigDecimal score;
  private BigDecimal sourceTotalScore;
  private BigDecimal scoreScale;
  private Integer difficulty;
  private String operationContent;
  private String studentPrepare;
  private String equipmentPrepare;
  private String scoringItemsJson;
  private String practicalDetailJson;
  private BigDecimal scoreDifference;
  private String scoringMethod;

  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getSourceQuestionId() { return sourceQuestionId; }
  public void setSourceQuestionId(Long sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }
  public Long getBlueprintId() { return blueprintId; }
  public void setBlueprintId(Long blueprintId) { this.blueprintId = blueprintId; }
  public Long getBlueprintNodeId() { return blueprintNodeId; }
  public void setBlueprintNodeId(Long blueprintNodeId) { this.blueprintNodeId = blueprintNodeId; }
  public String getBlueprintFullCode() { return blueprintFullCode; }
  public void setBlueprintFullCode(String blueprintFullCode) { this.blueprintFullCode = blueprintFullCode; }
  public String getBlueprintPath() { return blueprintPath; }
  public void setBlueprintPath(String blueprintPath) { this.blueprintPath = blueprintPath; }
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
  public BigDecimal getSourceTotalScore() { return sourceTotalScore; }
  public void setSourceTotalScore(BigDecimal sourceTotalScore) { this.sourceTotalScore = sourceTotalScore; }
  public BigDecimal getScoreScale() { return scoreScale; }
  public void setScoreScale(BigDecimal scoreScale) { this.scoreScale = scoreScale; }
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
  public String getPracticalDetailJson() { return practicalDetailJson; }
  public void setPracticalDetailJson(String practicalDetailJson) { this.practicalDetailJson = practicalDetailJson; }
  public BigDecimal getScoreDifference() { return scoreDifference; }
  public void setScoreDifference(BigDecimal scoreDifference) { this.scoreDifference = scoreDifference; }
  public String getScoringMethod() { return scoringMethod; }
  public void setScoringMethod(String scoringMethod) { this.scoringMethod = scoringMethod; }
}
