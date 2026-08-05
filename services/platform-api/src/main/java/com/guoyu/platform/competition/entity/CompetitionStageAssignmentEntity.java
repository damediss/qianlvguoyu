package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_stage_assignment")
public class CompetitionStageAssignmentEntity extends BaseEntity {
  private Long planId;
  private Long rosterSnapshotId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long contestantMemberId;
  private Long sessionId;
  private String assignmentType;
  private Long resourceId;
  private String assignmentStatus;
  private String sourceType;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Long getRosterSnapshotId() { return rosterSnapshotId; }
  public void setRosterSnapshotId(Long value) { rosterSnapshotId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long value) { sessionId = value; }
  public String getAssignmentType() { return assignmentType; }
  public void setAssignmentType(String value) { assignmentType = value; }
  public Long getResourceId() { return resourceId; }
  public void setResourceId(Long value) { resourceId = value; }
  public String getAssignmentStatus() { return assignmentStatus; }
  public void setAssignmentStatus(String value) { assignmentStatus = value; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String value) { sourceType = value; }
}
