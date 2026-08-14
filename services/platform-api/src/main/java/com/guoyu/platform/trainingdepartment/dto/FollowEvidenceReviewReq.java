package com.guoyu.platform.trainingdepartment.dto;

/** 审核课堂影像 */
public class FollowEvidenceReviewReq {
  /** approved / rejected */
  private String reviewStatus;
  private String rejectReason;

  public String getReviewStatus() { return reviewStatus; }
  public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
