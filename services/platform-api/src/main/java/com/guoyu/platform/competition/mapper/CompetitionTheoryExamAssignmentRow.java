package com.guoyu.platform.competition.mapper;

import java.time.LocalDateTime;

public class CompetitionTheoryExamAssignmentRow {
  private Long assignmentId;
  private Long tenantId;
  private Long scheduleVersionId;
  private Long sessionId;
  private Long paperSnapshotId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long contestantProfileId;
  private Long contestantMemberId;
  private Long playerId;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String sessionName;
  private String executionStatus;
  private String eventName;
  private String workTypeName;
  private String roomName;
  private String address;
  private String seatNo;
  private Long scoreRuleId;
  private Integer questionCount;

  public Long getAssignmentId() { return assignmentId; }
  public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getScheduleVersionId() { return scheduleVersionId; }
  public void setScheduleVersionId(Long scheduleVersionId) { this.scheduleVersionId = scheduleVersionId; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) { this.contestantProfileId = contestantProfileId; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) { this.contestantMemberId = contestantMemberId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
  public LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
  public String getSessionName() { return sessionName; }
  public void setSessionName(String sessionName) { this.sessionName = sessionName; }
  public String getExecutionStatus() { return executionStatus; }
  public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }
  public String getEventName() { return eventName; }
  public void setEventName(String eventName) { this.eventName = eventName; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public String getRoomName() { return roomName; }
  public void setRoomName(String roomName) { this.roomName = roomName; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getSeatNo() { return seatNo; }
  public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
  public Long getScoreRuleId() { return scoreRuleId; }
  public void setScoreRuleId(Long scoreRuleId) { this.scoreRuleId = scoreRuleId; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
}
