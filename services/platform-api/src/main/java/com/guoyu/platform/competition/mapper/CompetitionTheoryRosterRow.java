package com.guoyu.platform.competition.mapper;

import java.time.LocalDateTime;

public class CompetitionTheoryRosterRow {

  private Long assignmentId;
  private Long scheduleVersionId;
  private Long eventId;
  private Long playerId;
  private Long sessionId;
  private String eventName;
  private String workTypeName;
  private String sessionName;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String roomName;
  private String address;
  private String seatNo;
  private String contestantNo;
  private String contestantName;
  private String orgName;

  public Long getAssignmentId() { return assignmentId; }
  public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
  public Long getScheduleVersionId() { return scheduleVersionId; }
  public void setScheduleVersionId(Long scheduleVersionId) {
    this.scheduleVersionId = scheduleVersionId;
  }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public String getEventName() { return eventName; }
  public void setEventName(String eventName) { this.eventName = eventName; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public String getSessionName() { return sessionName; }
  public void setSessionName(String sessionName) { this.sessionName = sessionName; }
  public LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
  public LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
  public String getRoomName() { return roomName; }
  public void setRoomName(String roomName) { this.roomName = roomName; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getSeatNo() { return seatNo; }
  public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
  public String getContestantNo() { return contestantNo; }
  public void setContestantNo(String contestantNo) { this.contestantNo = contestantNo; }
  public String getContestantName() { return contestantName; }
  public void setContestantName(String contestantName) { this.contestantName = contestantName; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
}
