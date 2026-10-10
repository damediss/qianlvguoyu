package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/** The work-type/level binding belonging to one player in one event. */
public class CompetitionPlayerWorkTypeVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long registrationId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long profileId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeId;
  private String eventWorkTypeName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeLevelId;
  private String levelCode;
  private String levelName;
  private String level;
  private String status;

  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getProfileId() { return profileId; }
  public void setProfileId(Long profileId) { this.profileId = profileId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getEventWorkTypeName() { return eventWorkTypeName; }
  public void setEventWorkTypeName(String eventWorkTypeName) { this.eventWorkTypeName = eventWorkTypeName; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
