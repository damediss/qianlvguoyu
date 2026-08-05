package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_referee_avoidance")
public class CompetitionRefereeAvoidanceEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long refereeMemberId;
  private Long contestantMemberId;
  private String relationType;
  private String reason;
  private Long configuredBy;
  private String status;
  private LocalDateTime configuredAt;
  private LocalDateTime endedAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public Long getRefereeMemberId() { return refereeMemberId; }
  public void setRefereeMemberId(Long refereeMemberId) { this.refereeMemberId = refereeMemberId; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public String getRelationType() { return relationType; }
  public void setRelationType(String relationType) { this.relationType = relationType; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Long getConfiguredBy() { return configuredBy; }
  public void setConfiguredBy(Long configuredBy) { this.configuredBy = configuredBy; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getConfiguredAt() { return configuredAt; }
  public void setConfiguredAt(LocalDateTime configuredAt) { this.configuredAt = configuredAt; }
  public LocalDateTime getEndedAt() { return endedAt; }
  public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
