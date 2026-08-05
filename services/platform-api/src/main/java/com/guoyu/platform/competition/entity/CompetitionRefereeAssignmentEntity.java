package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_referee_assignment")
public class CompetitionRefereeAssignmentEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long refereeMemberId;
  private String assignmentRole;
  private Long contestantMemberId;
  private String status;
  private Long replacedAssignmentId;
  private String replacementReason;
  private LocalDateTime assignedAt;
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
  public String getAssignmentRole() { return assignmentRole; }
  public void setAssignmentRole(String assignmentRole) { this.assignmentRole = assignmentRole; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getReplacedAssignmentId() { return replacedAssignmentId; }
  public void setReplacedAssignmentId(Long replacedAssignmentId) {
    this.replacedAssignmentId = replacedAssignmentId;
  }
  public String getReplacementReason() { return replacementReason; }
  public void setReplacementReason(String replacementReason) {
    this.replacementReason = replacementReason;
  }
  public LocalDateTime getAssignedAt() { return assignedAt; }
  public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
  public LocalDateTime getEndedAt() { return endedAt; }
  public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
