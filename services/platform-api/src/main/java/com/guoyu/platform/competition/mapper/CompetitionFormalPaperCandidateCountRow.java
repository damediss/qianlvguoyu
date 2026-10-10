package com.guoyu.platform.competition.mapper;

public class CompetitionFormalPaperCandidateCountRow {

  private Long paperSnapshotId;
  private Long total;

  public Long getPaperSnapshotId() {
    return paperSnapshotId;
  }

  public void setPaperSnapshotId(Long paperSnapshotId) {
    this.paperSnapshotId = paperSnapshotId;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public int totalOrZero() {
    return total == null ? 0 : total.intValue();
  }
}
