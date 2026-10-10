package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class CompetitionRefereeVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long memberId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long personId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long userId;
  private String accountUsername;
  private String eventName;
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
  private String status;
  private LocalDateTime createdAt;
  private List<CompetitionRefereeQualificationVO> qualifications = new ArrayList<>();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getMemberId() { return memberId; }
  public void setMemberId(Long memberId) { this.memberId = memberId; }
  public Long getPersonId() { return personId; }
  public void setPersonId(Long personId) { this.personId = personId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getAccountUsername() { return accountUsername; }
  public void setAccountUsername(String accountUsername) { this.accountUsername = accountUsername; }
  public String getEventName() { return eventName; }
  public void setEventName(String eventName) { this.eventName = eventName; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getEventWorkTypeName() { return eventWorkTypeName; }
  public void setEventWorkTypeName(String eventWorkTypeName) { this.eventWorkTypeName = eventWorkTypeName; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) {
    this.eventWorkTypeLevelId = eventWorkTypeLevelId;
  }
  public String getEventWorkTypeLevelCode() { return eventWorkTypeLevelCode; }
  public void setEventWorkTypeLevelCode(String eventWorkTypeLevelCode) {
    this.eventWorkTypeLevelCode = eventWorkTypeLevelCode;
  }
  public String getEventWorkTypeLevelName() { return eventWorkTypeLevelName; }
  public void setEventWorkTypeLevelName(String eventWorkTypeLevelName) {
    this.eventWorkTypeLevelName = eventWorkTypeLevelName;
  }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getIdCardNo() { return idCardNo; }
  public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
  public String getMobile() { return mobile; }
  public void setMobile(String mobile) { this.mobile = mobile; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public List<CompetitionRefereeQualificationVO> getQualifications() { return qualifications; }
  public void setQualifications(List<CompetitionRefereeQualificationVO> qualifications) {
    this.qualifications = qualifications == null ? new ArrayList<>() : new ArrayList<>(qualifications);
  }
}
