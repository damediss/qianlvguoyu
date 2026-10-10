package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CompetitionScoreRuleVO {

  private Long id;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String scoringSystem;
  private String theoryTemplate;
  private BigDecimal theoryWeight;
  private BigDecimal simulationPracticalWeight;
  private BigDecimal simulationWeight;
  private BigDecimal practicalWeight;
  private String status;
  private Integer ruleVersion;
  private LocalDateTime publishedAt;
  private Long publishedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getScoringSystem() { return scoringSystem; }
  public void setScoringSystem(String scoringSystem) { this.scoringSystem = scoringSystem; }
  public String getTheoryTemplate() { return theoryTemplate; }
  public void setTheoryTemplate(String theoryTemplate) { this.theoryTemplate = theoryTemplate; }
  public BigDecimal getTheoryWeight() { return theoryWeight; }
  public void setTheoryWeight(BigDecimal theoryWeight) { this.theoryWeight = theoryWeight; }
  public BigDecimal getSimulationPracticalWeight() { return simulationPracticalWeight; }
  public void setSimulationPracticalWeight(BigDecimal simulationPracticalWeight) {
    this.simulationPracticalWeight = simulationPracticalWeight;
  }
  public BigDecimal getSimulationWeight() { return simulationWeight; }
  public void setSimulationWeight(BigDecimal simulationWeight) { this.simulationWeight = simulationWeight; }
  public BigDecimal getPracticalWeight() { return practicalWeight; }
  public void setPracticalWeight(BigDecimal practicalWeight) { this.practicalWeight = practicalWeight; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getRuleVersion() { return ruleVersion; }
  public void setRuleVersion(Integer ruleVersion) { this.ruleVersion = ruleVersion; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
