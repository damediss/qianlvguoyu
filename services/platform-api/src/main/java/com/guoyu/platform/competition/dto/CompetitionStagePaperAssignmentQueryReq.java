package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionStagePaperAssignmentQueryReq extends PageQuery {

  private String keyword;
  private Long orgId;
  private Long paperSnapshotId;
  private String assignmentStatus;

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public String getAssignmentStatus() { return assignmentStatus; }
  public void setAssignmentStatus(String assignmentStatus) { this.assignmentStatus = assignmentStatus; }
}
