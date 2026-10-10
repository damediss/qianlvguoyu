package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_score_source")
public class CompetitionScoreSourceEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long contestantMemberId;
  private String sourceType;
  private BigDecimal rawScore;
  private String confirmationStatus;
  private String sourceSystem;
  private String sourceReference;
  private Long scoreRuleId;
  private Integer revisionNo;
  private Integer currentFlag;
  private LocalDateTime achievedAt;
  private LocalDateTime confirmedAt;
  private Long confirmedBy;

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
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  public BigDecimal getRawScore() { return rawScore; }
  public void setRawScore(BigDecimal rawScore) { this.rawScore = rawScore; }
  public String getConfirmationStatus() { return confirmationStatus; }
  public void setConfirmationStatus(String confirmationStatus) {
    this.confirmationStatus = confirmationStatus;
  }
  public String getSourceSystem() { return sourceSystem; }
  public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
  public String getSourceReference() { return sourceReference; }
  public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
  public Long getScoreRuleId() { return scoreRuleId; }
  public void setScoreRuleId(Long scoreRuleId) { this.scoreRuleId = scoreRuleId; }
  public Integer getRevisionNo() { return revisionNo; }
  public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }
  public Integer getCurrentFlag() { return currentFlag; }
  public void setCurrentFlag(Integer currentFlag) { this.currentFlag = currentFlag; }
  public LocalDateTime getAchievedAt() { return achievedAt; }
  public void setAchievedAt(LocalDateTime achievedAt) { this.achievedAt = achievedAt; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
}
