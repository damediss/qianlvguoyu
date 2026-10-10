package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_score_unlock_request")
public class CompetitionScoreUnlockRequestEntity extends BaseEntity {

  private Long practicalScoreId;
  private Integer requestNo;
  private String requestReason;
  private Long evidenceFileId;
  private Long applicantMemberId;
  private String status;
  private Long decidedBy;
  private String decisionComment;
  private LocalDateTime requestedAt;
  private LocalDateTime decidedAt;

  public Long getPracticalScoreId() { return practicalScoreId; }
  public void setPracticalScoreId(Long practicalScoreId) {
    this.practicalScoreId = practicalScoreId;
  }
  public Integer getRequestNo() { return requestNo; }
  public void setRequestNo(Integer requestNo) { this.requestNo = requestNo; }
  public String getRequestReason() { return requestReason; }
  public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
  public Long getEvidenceFileId() { return evidenceFileId; }
  public void setEvidenceFileId(Long evidenceFileId) { this.evidenceFileId = evidenceFileId; }
  public Long getApplicantMemberId() { return applicantMemberId; }
  public void setApplicantMemberId(Long applicantMemberId) {
    this.applicantMemberId = applicantMemberId;
  }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getDecidedBy() { return decidedBy; }
  public void setDecidedBy(Long decidedBy) { this.decidedBy = decidedBy; }
  public String getDecisionComment() { return decisionComment; }
  public void setDecisionComment(String decisionComment) {
    this.decisionComment = decisionComment;
  }
  public LocalDateTime getRequestedAt() { return requestedAt; }
  public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
  public LocalDateTime getDecidedAt() { return decidedAt; }
  public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
