package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;

public class CompetitionRefereeAssignmentWorkspaceVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long stageId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeLevelId;
  private String stageName;
  private String workTypeName;
  private String levelName;
  private List<CompetitionRefereeVO> referees = new ArrayList<>();
  private List<CompetitionRefereeGroupVO> groups = new ArrayList<>();
  private List<CompetitionRefereeCandidateVO> candidates = new ArrayList<>();

  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) {
    this.eventWorkTypeLevelId = eventWorkTypeLevelId;
  }
  public String getStageName() { return stageName; }
  public void setStageName(String stageName) { this.stageName = stageName; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public List<CompetitionRefereeVO> getReferees() { return referees; }
  public void setReferees(List<CompetitionRefereeVO> referees) {
    this.referees = referees == null ? new ArrayList<>() : new ArrayList<>(referees);
  }
  public List<CompetitionRefereeGroupVO> getGroups() { return groups; }
  public void setGroups(List<CompetitionRefereeGroupVO> groups) {
    this.groups = groups == null ? new ArrayList<>() : new ArrayList<>(groups);
  }
  public List<CompetitionRefereeCandidateVO> getCandidates() { return candidates; }
  public void setCandidates(List<CompetitionRefereeCandidateVO> candidates) {
    this.candidates = candidates == null ? new ArrayList<>() : new ArrayList<>(candidates);
  }
}
