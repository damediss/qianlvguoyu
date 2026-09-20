package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class CompetitionRefereeQualificationVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long workTypeId;
  private String workTypeName;
  private String levelFromCode;
  private String levelToCode;
  private String levelFromName;
  private String levelToName;
  private String levelRange;
  /** Legacy single-level alias retained for older clients. */
  private String levelCode;
  private String levelName;

  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public String getLevelFromCode() { return levelFromCode; }
  public void setLevelFromCode(String levelFromCode) { this.levelFromCode = levelFromCode; }
  public String getLevelToCode() { return levelToCode; }
  public void setLevelToCode(String levelToCode) { this.levelToCode = levelToCode; }
  public String getLevelFromName() { return levelFromName; }
  public void setLevelFromName(String levelFromName) { this.levelFromName = levelFromName; }
  public String getLevelToName() { return levelToName; }
  public void setLevelToName(String levelToName) { this.levelToName = levelToName; }
  public String getLevelRange() { return levelRange; }
  public void setLevelRange(String levelRange) { this.levelRange = levelRange; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
}
