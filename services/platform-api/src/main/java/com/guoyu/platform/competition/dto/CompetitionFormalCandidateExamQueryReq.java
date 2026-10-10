package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionFormalCandidateExamQueryReq extends PageQuery {

  private String keyword;
  private Long paperSnapshotId;
  private String status;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public Long getPaperSnapshotId() {
    return paperSnapshotId;
  }

  public void setPaperSnapshotId(Long paperSnapshotId) {
    this.paperSnapshotId = paperSnapshotId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
