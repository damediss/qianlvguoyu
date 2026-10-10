package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_formal_candidate_exam")
public class CompetitionFormalCandidateExamEntity extends BaseEntity {

  private Long examSessionId;
  private Long contestantProfileId;
  private Long contestantMemberId;
  private Long registrationId;
  private Long playerId;
  private String candidateNo;
  private String candidateName;
  private Long paperSnapshotId;
  private Long sourcePaperId;
  private String paperCode;
  private String paperName;
  private String status;
  private LocalDateTime enterTime;
  private LocalDateTime startTime;
  private LocalDateTime submitTime;
  private String submitType;
  private Integer answeredCount;
  private Integer totalQuestionCount;
  private LocalDateTime lastActiveTime;
  private String forceSubmitReason;
  private Long forceSubmittedBy;
  private LocalDateTime absentTime;
  private String absentReason;

  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) {
    this.contestantProfileId = contestantProfileId;
  }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getCandidateNo() { return candidateNo; }
  public void setCandidateNo(String candidateNo) { this.candidateNo = candidateNo; }
  public String getCandidateName() { return candidateName; }
  public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getSourcePaperId() { return sourcePaperId; }
  public void setSourcePaperId(Long sourcePaperId) { this.sourcePaperId = sourcePaperId; }
  public String getPaperCode() { return paperCode; }
  public void setPaperCode(String paperCode) { this.paperCode = paperCode; }
  public String getPaperName() { return paperName; }
  public void setPaperName(String paperName) { this.paperName = paperName; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getEnterTime() { return enterTime; }
  public void setEnterTime(LocalDateTime enterTime) { this.enterTime = enterTime; }
  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
  public LocalDateTime getSubmitTime() { return submitTime; }
  public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
  public String getSubmitType() { return submitType; }
  public void setSubmitType(String submitType) { this.submitType = submitType; }
  public Integer getAnsweredCount() { return answeredCount; }
  public void setAnsweredCount(Integer answeredCount) { this.answeredCount = answeredCount; }
  public Integer getTotalQuestionCount() { return totalQuestionCount; }
  public void setTotalQuestionCount(Integer totalQuestionCount) {
    this.totalQuestionCount = totalQuestionCount;
  }
  public LocalDateTime getLastActiveTime() { return lastActiveTime; }
  public void setLastActiveTime(LocalDateTime lastActiveTime) {
    this.lastActiveTime = lastActiveTime;
  }
  public String getForceSubmitReason() { return forceSubmitReason; }
  public void setForceSubmitReason(String forceSubmitReason) {
    this.forceSubmitReason = forceSubmitReason;
  }
  public Long getForceSubmittedBy() { return forceSubmittedBy; }
  public void setForceSubmittedBy(Long forceSubmittedBy) {
    this.forceSubmittedBy = forceSubmittedBy;
  }
  public LocalDateTime getAbsentTime() { return absentTime; }
  public void setAbsentTime(LocalDateTime absentTime) { this.absentTime = absentTime; }
  public String getAbsentReason() { return absentReason; }
  public void setAbsentReason(String absentReason) { this.absentReason = absentReason; }
}
