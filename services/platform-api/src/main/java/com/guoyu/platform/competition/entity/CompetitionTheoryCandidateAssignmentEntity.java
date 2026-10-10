package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_theory_candidate_assignment")
public class CompetitionTheoryCandidateAssignmentEntity extends BaseEntity {

  private Long scheduleVersionId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long sessionId;
  private Long sessionRoomId;
  private Long roomId;
  private Long contestantProfileId;
  private Long contestantMemberId;
  private Long registrationId;
  private Long playerId;
  private String seatNo;
  private String randomBatchId;
  private String allocationSource;
  private String status;

  public Long getScheduleVersionId() { return scheduleVersionId; }
  public void setScheduleVersionId(Long scheduleVersionId) { this.scheduleVersionId = scheduleVersionId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public Long getSessionRoomId() { return sessionRoomId; }
  public void setSessionRoomId(Long sessionRoomId) { this.sessionRoomId = sessionRoomId; }
  public Long getRoomId() { return roomId; }
  public void setRoomId(Long roomId) { this.roomId = roomId; }
  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) { this.contestantProfileId = contestantProfileId; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) { this.contestantMemberId = contestantMemberId; }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getSeatNo() { return seatNo; }
  public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
  public String getRandomBatchId() { return randomBatchId; }
  public void setRandomBatchId(String randomBatchId) { this.randomBatchId = randomBatchId; }
  public String getAllocationSource() { return allocationSource; }
  public void setAllocationSource(String allocationSource) { this.allocationSource = allocationSource; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
