package com.guoyu.platform.competition.mapper;

import java.time.LocalDateTime;

public class CompetitionPlayerQueryRow {

  private Long registrationId;
  private Long eventId;
  private String eventName;
  private Long orgId;
  private String orgName;
  private Long rosterItemId;
  private Long playerId;
  private Long memberId;
  private Long profileId;
  private Long eventWorkTypeId;
  private String eventWorkTypeName;
  private String employeeNo;
  private String name;
  private String workType;
  private String level;
  private String status;
  private String playerStatus;
  private String idCardNo;
  private String mobile;
  private LocalDateTime createdAt;

  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public String getEventName() { return eventName; }
  public void setEventName(String eventName) { this.eventName = eventName; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public Long getRosterItemId() { return rosterItemId; }
  public void setRosterItemId(Long rosterItemId) { this.rosterItemId = rosterItemId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public Long getMemberId() { return memberId; }
  public void setMemberId(Long memberId) { this.memberId = memberId; }
  public Long getProfileId() { return profileId; }
  public void setProfileId(Long profileId) { this.profileId = profileId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getEventWorkTypeName() { return eventWorkTypeName; }
  public void setEventWorkTypeName(String eventWorkTypeName) { this.eventWorkTypeName = eventWorkTypeName; }
  public String getEmployeeNo() { return employeeNo; }
  public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getWorkType() { return workType; }
  public void setWorkType(String workType) { this.workType = workType; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getPlayerStatus() { return playerStatus; }
  public void setPlayerStatus(String playerStatus) { this.playerStatus = playerStatus; }
  public String getIdCardNo() { return idCardNo; }
  public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
  public String getMobile() { return mobile; }
  public void setMobile(String mobile) { this.mobile = mobile; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
