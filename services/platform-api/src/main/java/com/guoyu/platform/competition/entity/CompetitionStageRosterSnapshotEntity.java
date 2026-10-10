package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_roster_snapshot")
public class CompetitionStageRosterSnapshotEntity extends BaseEntity {
  private Long planId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Integer snapshotVersion;
  private String sourceType;
  private Long sourcePromotionSnapshotId;
  private String status;
  private LocalDateTime generatedAt;
  private LocalDateTime publishedAt;
  private Long publishedBy;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Integer getSnapshotVersion() { return snapshotVersion; }
  public void setSnapshotVersion(Integer value) { snapshotVersion = value; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String value) { sourceType = value; }
  public Long getSourcePromotionSnapshotId() { return sourcePromotionSnapshotId; }
  public void setSourcePromotionSnapshotId(Long value) { sourcePromotionSnapshotId = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public LocalDateTime getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(LocalDateTime value) { generatedAt = value; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime value) { publishedAt = value; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long value) { publishedBy = value; }
}
