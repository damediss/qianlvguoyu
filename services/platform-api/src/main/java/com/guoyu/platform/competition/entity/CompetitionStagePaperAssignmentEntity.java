package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_paper_assignment")
public class CompetitionStagePaperAssignmentEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private Long contestantProfileId;
  private Long contestantMemberId;
  private Long registrationId;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private Long paperSnapshotId;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private Long assessmentSheetId;
  private String allocationSource;
  private String status;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private Long confirmedBy;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private LocalDateTime confirmedAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) {
    this.eventWorkTypeLevelId = eventWorkTypeLevelId;
  }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
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
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getAssessmentSheetId() { return assessmentSheetId; }
  public void setAssessmentSheetId(Long assessmentSheetId) { this.assessmentSheetId = assessmentSheetId; }
  public String getAllocationSource() { return allocationSource; }
  public void setAllocationSource(String allocationSource) { this.allocationSource = allocationSource; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}
