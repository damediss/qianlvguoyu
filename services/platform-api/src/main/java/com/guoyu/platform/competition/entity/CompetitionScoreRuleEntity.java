package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_score_rule")
public class CompetitionScoreRuleEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private String scoringSystem;
  private String theoryTemplate;
  private BigDecimal theoryWeight;
  private BigDecimal simulationPracticalWeight;
  private String status;
  private Integer ruleVersion;
  private LocalDateTime publishedAt;
  private Long publishedBy;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
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
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getRuleVersion() { return ruleVersion; }
  public void setRuleVersion(Integer ruleVersion) { this.ruleVersion = ruleVersion; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
}
