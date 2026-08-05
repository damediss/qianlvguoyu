package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_total_score_snapshot")
public class CompetitionTotalScoreSnapshotEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long contestantMemberId;
  private Long scoreRuleId;
  private Long theorySourceId;
  private Long simulationSourceId;
  private Long practicalSourceId;
  private BigDecimal theoryScore;
  private BigDecimal simulationScore;
  private BigDecimal practicalScore;
  private BigDecimal simulationPracticalScore;
  private BigDecimal theoryWeight;
  private BigDecimal simulationWeight;
  private BigDecimal practicalWeight;
  private BigDecimal simulationPracticalWeight;
  private BigDecimal totalScore;
  private Integer calculationVersion;
  private String confirmationStatus;
  private Integer currentFlag;
  private LocalDateTime calculatedAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public Long getScoreRuleId() { return scoreRuleId; }
  public void setScoreRuleId(Long scoreRuleId) { this.scoreRuleId = scoreRuleId; }
  public Long getTheorySourceId() { return theorySourceId; }
  public void setTheorySourceId(Long theorySourceId) { this.theorySourceId = theorySourceId; }
  public Long getSimulationSourceId() { return simulationSourceId; }
  public void setSimulationSourceId(Long simulationSourceId) {
    this.simulationSourceId = simulationSourceId;
  }
  public Long getPracticalSourceId() { return practicalSourceId; }
  public void setPracticalSourceId(Long practicalSourceId) {
    this.practicalSourceId = practicalSourceId;
  }
  public BigDecimal getTheoryScore() { return theoryScore; }
  public void setTheoryScore(BigDecimal theoryScore) { this.theoryScore = theoryScore; }
  public BigDecimal getSimulationScore() { return simulationScore; }
  public void setSimulationScore(BigDecimal simulationScore) {
    this.simulationScore = simulationScore;
  }
  public BigDecimal getPracticalScore() { return practicalScore; }
  public void setPracticalScore(BigDecimal practicalScore) { this.practicalScore = practicalScore; }
  public BigDecimal getSimulationPracticalScore() { return simulationPracticalScore; }
  public void setSimulationPracticalScore(BigDecimal simulationPracticalScore) {
    this.simulationPracticalScore = simulationPracticalScore;
  }
  public BigDecimal getTheoryWeight() { return theoryWeight; }
  public void setTheoryWeight(BigDecimal theoryWeight) { this.theoryWeight = theoryWeight; }
  public BigDecimal getSimulationWeight() { return simulationWeight; }
  public void setSimulationWeight(BigDecimal simulationWeight) { this.simulationWeight = simulationWeight; }
  public BigDecimal getPracticalWeight() { return practicalWeight; }
  public void setPracticalWeight(BigDecimal practicalWeight) { this.practicalWeight = practicalWeight; }
  public BigDecimal getSimulationPracticalWeight() { return simulationPracticalWeight; }
  public void setSimulationPracticalWeight(BigDecimal simulationPracticalWeight) {
    this.simulationPracticalWeight = simulationPracticalWeight;
  }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public Integer getCalculationVersion() { return calculationVersion; }
  public void setCalculationVersion(Integer calculationVersion) {
    this.calculationVersion = calculationVersion;
  }
  public String getConfirmationStatus() { return confirmationStatus; }
  public void setConfirmationStatus(String confirmationStatus) {
    this.confirmationStatus = confirmationStatus;
  }
  public Integer getCurrentFlag() { return currentFlag; }
  public void setCurrentFlag(Integer currentFlag) { this.currentFlag = currentFlag; }
  public LocalDateTime getCalculatedAt() { return calculatedAt; }
  public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
