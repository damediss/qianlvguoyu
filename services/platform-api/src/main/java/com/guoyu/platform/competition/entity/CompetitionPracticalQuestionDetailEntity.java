package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_practical_question_detail")
public class CompetitionPracticalQuestionDetailEntity extends BaseEntity {

  private Long questionId;
  private Integer durationMinutes;
  private BigDecimal scoreDifference;
  private BigDecimal totalScore;
  private String scoringMethod;
  private String assessmentRequirementsHtml;
  private String examRoomPrepareHtml;
  private String candidatePrepareHtml;
  private String negativeItemsHtml;
  private String questionMaterialsHtml;
  /** Legacy column aliases retained while old snapshots/imports are migrated. */
  private String operationContent;
  private String studentPrepare;
  private String equipmentPrepare;

  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
  public BigDecimal getScoreDifference() { return scoreDifference; }
  public void setScoreDifference(BigDecimal scoreDifference) { this.scoreDifference = scoreDifference; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public String getScoringMethod() { return scoringMethod; }
  public void setScoringMethod(String scoringMethod) { this.scoringMethod = scoringMethod; }
  public String getAssessmentRequirementsHtml() { return assessmentRequirementsHtml; }
  public void setAssessmentRequirementsHtml(String value) { this.assessmentRequirementsHtml = value; }
  public String getExamRoomPrepareHtml() { return examRoomPrepareHtml; }
  public void setExamRoomPrepareHtml(String value) { this.examRoomPrepareHtml = value; }
  public String getCandidatePrepareHtml() { return candidatePrepareHtml; }
  public void setCandidatePrepareHtml(String value) { this.candidatePrepareHtml = value; }
  public String getNegativeItemsHtml() { return negativeItemsHtml; }
  public void setNegativeItemsHtml(String value) { this.negativeItemsHtml = value; }
  public String getQuestionMaterialsHtml() { return questionMaterialsHtml; }
  public void setQuestionMaterialsHtml(String value) { this.questionMaterialsHtml = value; }
  public String getOperationContent() { return operationContent; }
  public void setOperationContent(String operationContent) { this.operationContent = operationContent; }
  public String getStudentPrepare() { return studentPrepare; }
  public void setStudentPrepare(String studentPrepare) { this.studentPrepare = studentPrepare; }
  public String getEquipmentPrepare() { return equipmentPrepare; }
  public void setEquipmentPrepare(String equipmentPrepare) { this.equipmentPrepare = equipmentPrepare; }
}
