package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_stage_roster_item")
public class CompetitionStageRosterItemEntity extends BaseEntity {
  private Long snapshotId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long contestantMemberId;
  private Long contestantProfileId;
  private Long registrationId;
  private String inclusionStatus;
  private String eligibilityStatus;
  private String reason;
  private Long sourcePromotionItemId;

  public Long getSnapshotId() { return snapshotId; }
  public void setSnapshotId(Long value) { snapshotId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long value) { contestantProfileId = value; }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long value) { registrationId = value; }
  public String getInclusionStatus() { return inclusionStatus; }
  public void setInclusionStatus(String value) { inclusionStatus = value; }
  public String getEligibilityStatus() { return eligibilityStatus; }
  public void setEligibilityStatus(String value) { eligibilityStatus = value; }
  public String getReason() { return reason; }
  public void setReason(String value) { reason = value; }
  public Long getSourcePromotionItemId() { return sourcePromotionItemId; }
  public void setSourcePromotionItemId(Long value) { sourcePromotionItemId = value; }
}
