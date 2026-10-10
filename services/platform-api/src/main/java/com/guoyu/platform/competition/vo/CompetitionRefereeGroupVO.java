package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;

public class CompetitionRefereeGroupVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long stageId;
  private Integer groupNo;
  private String groupName;
  private String status;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long leaderRefereeProfileId;
  private Boolean bindable;
  private Boolean releasable;
  private String unavailableReason;
  private String conflictStageName;
  private String occupiedEventName;
  private List<CompetitionRefereeGroupMemberVO> members = new ArrayList<>();
  private List<CompetitionRefereeGroupBoundTaskVO> boundTasks = new ArrayList<>();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Integer getGroupNo() { return groupNo; }
  public void setGroupNo(Integer groupNo) { this.groupNo = groupNo; }
  public String getGroupName() { return groupName; }
  public void setGroupName(String groupName) { this.groupName = groupName; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getLeaderRefereeProfileId() { return leaderRefereeProfileId; }
  public void setLeaderRefereeProfileId(Long leaderRefereeProfileId) {
    this.leaderRefereeProfileId = leaderRefereeProfileId;
  }
  public Boolean getBindable() { return bindable; }
  public void setBindable(Boolean bindable) { this.bindable = bindable; }
  public Boolean getReleasable() { return releasable; }
  public void setReleasable(Boolean releasable) { this.releasable = releasable; }
  public String getUnavailableReason() { return unavailableReason; }
  public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }
  public String getConflictStageName() { return conflictStageName; }
  public void setConflictStageName(String conflictStageName) { this.conflictStageName = conflictStageName; }
  public String getOccupiedEventName() { return occupiedEventName; }
  public void setOccupiedEventName(String occupiedEventName) { this.occupiedEventName = occupiedEventName; }
  public List<CompetitionRefereeGroupMemberVO> getMembers() { return members; }
  public void setMembers(List<CompetitionRefereeGroupMemberVO> members) {
    this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
  }
  public List<CompetitionRefereeGroupBoundTaskVO> getBoundTasks() { return boundTasks; }
  public void setBoundTasks(List<CompetitionRefereeGroupBoundTaskVO> boundTasks) {
    this.boundTasks = boundTasks == null ? new ArrayList<>() : new ArrayList<>(boundTasks);
  }
}
