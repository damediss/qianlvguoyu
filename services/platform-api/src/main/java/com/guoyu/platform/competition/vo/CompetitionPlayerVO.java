package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompetitionPlayerVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long registrationId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventId;
  private String eventName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long orgId;
  private String orgName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long playerId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long memberId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long rosterItemId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeId;
  private String eventWorkTypeName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeLevelId;
  private String eventWorkTypeLevelCode;
  private String eventWorkTypeLevelName;
  private String name;
  private String idCardNo;
  private String mobile;
  private String level;
  private String status;
  private String playerStatus;
  private LocalDateTime createdAt;
  private List<CompetitionPlayerWorkTypeVO> workTypes = new ArrayList<>();

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
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public Long getMemberId() { return memberId; }
  public void setMemberId(Long memberId) { this.memberId = memberId; }
  public Long getRosterItemId() { return rosterItemId; }
  public void setRosterItemId(Long rosterItemId) { this.rosterItemId = rosterItemId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getEventWorkTypeName() { return eventWorkTypeName; }
  public void setEventWorkTypeName(String eventWorkTypeName) { this.eventWorkTypeName = eventWorkTypeName; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getEventWorkTypeLevelCode() { return eventWorkTypeLevelCode; }
  public void setEventWorkTypeLevelCode(String eventWorkTypeLevelCode) { this.eventWorkTypeLevelCode = eventWorkTypeLevelCode; }
  public String getEventWorkTypeLevelName() { return eventWorkTypeLevelName; }
  public void setEventWorkTypeLevelName(String eventWorkTypeLevelName) { this.eventWorkTypeLevelName = eventWorkTypeLevelName; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getIdCardNo() { return idCardNo; }
  public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
  public String getMobile() { return mobile; }
  public void setMobile(String mobile) { this.mobile = mobile; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getPlayerStatus() { return playerStatus; }
  public void setPlayerStatus(String playerStatus) { this.playerStatus = playerStatus; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public List<CompetitionPlayerWorkTypeVO> getWorkTypes() { return workTypes; }
  public void setWorkTypes(List<CompetitionPlayerWorkTypeVO> workTypes) {
    this.workTypes = workTypes == null ? new ArrayList<>() : new ArrayList<>(workTypes);
  }
}
