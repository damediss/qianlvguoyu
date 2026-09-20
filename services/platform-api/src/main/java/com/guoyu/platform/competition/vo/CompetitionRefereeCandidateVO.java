package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;

public class CompetitionRefereeCandidateVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long contestantProfileId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long contestantMemberId;
  private String contestantNo;
  private String name;
  private String orgName;
  private String workTypeName;
  private String levelCode;
  private String levelName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long paperSnapshotId;
  private String paperCode;
  private String paperTitle;
  private String assignmentStatus;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long refereeGroupId;
  private String refereeGroupName;
  private String refereeGroupStatus;
  private List<CompetitionRefereeAssignmentVO> assignments = new ArrayList<>();

  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) {
    this.contestantProfileId = contestantProfileId;
  }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public String getContestantNo() { return contestantNo; }
  public void setContestantNo(String contestantNo) { this.contestantNo = contestantNo; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public String getPaperCode() { return paperCode; }
  public void setPaperCode(String paperCode) { this.paperCode = paperCode; }
  public String getPaperTitle() { return paperTitle; }
  public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }
  public String getAssignmentStatus() { return assignmentStatus; }
  public void setAssignmentStatus(String assignmentStatus) { this.assignmentStatus = assignmentStatus; }
  public Long getRefereeGroupId() { return refereeGroupId; }
  public void setRefereeGroupId(Long refereeGroupId) { this.refereeGroupId = refereeGroupId; }
  public String getRefereeGroupName() { return refereeGroupName; }
  public void setRefereeGroupName(String refereeGroupName) { this.refereeGroupName = refereeGroupName; }
  public String getRefereeGroupStatus() { return refereeGroupStatus; }
  public void setRefereeGroupStatus(String refereeGroupStatus) { this.refereeGroupStatus = refereeGroupStatus; }
  public List<CompetitionRefereeAssignmentVO> getAssignments() { return assignments; }
  public void setAssignments(List<CompetitionRefereeAssignmentVO> assignments) {
    this.assignments = assignments == null ? new ArrayList<>() : new ArrayList<>(assignments);
  }
}
