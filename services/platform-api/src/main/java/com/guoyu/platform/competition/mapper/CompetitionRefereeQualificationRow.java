package com.guoyu.platform.competition.mapper;

public class CompetitionRefereeQualificationRow {

  private Long profileId;
  private Long workTypeId;
  private String workTypeName;
  private String levelFromCode;
  private String levelToCode;
  /** Legacy response alias for older consumers. */
  private String levelCode;

  public Long getProfileId() { return profileId; }
  public void setProfileId(Long profileId) { this.profileId = profileId; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public String getLevelFromCode() { return levelFromCode; }
  public void setLevelFromCode(String levelFromCode) { this.levelFromCode = levelFromCode; }
  public String getLevelToCode() { return levelToCode; }
  public void setLevelToCode(String levelToCode) { this.levelToCode = levelToCode; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
}
