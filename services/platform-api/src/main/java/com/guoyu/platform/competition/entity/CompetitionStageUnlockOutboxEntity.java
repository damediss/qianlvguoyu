package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_unlock_outbox")
public class CompetitionStageUnlockOutboxEntity extends BaseEntity {
  private Long sourceStageId; private Long targetStageId; private Long eventWorkTypeId; private Long eventWorkTypeLevelId;
  private Long promotionSnapshotId; private Long contestantMemberId; private String eventType; private String status; private Integer attempts; private String lastError; private LocalDateTime processedAt;
  public Long getSourceStageId() { return sourceStageId; } public void setSourceStageId(Long v) { sourceStageId = v; }
  public Long getTargetStageId() { return targetStageId; } public void setTargetStageId(Long v) { targetStageId = v; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; } public void setEventWorkTypeId(Long v) { eventWorkTypeId = v; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; } public void setEventWorkTypeLevelId(Long v) { eventWorkTypeLevelId = v; }
  public Long getPromotionSnapshotId() { return promotionSnapshotId; } public void setPromotionSnapshotId(Long v) { promotionSnapshotId = v; }
  public Long getContestantMemberId() { return contestantMemberId; } public void setContestantMemberId(Long v) { contestantMemberId = v; }
  public String getEventType() { return eventType; } public void setEventType(String v) { eventType = v; }
  public String getStatus() { return status; } public void setStatus(String v) { status = v; }
  public Integer getAttempts() { return attempts; } public void setAttempts(Integer v) { attempts = v; }
  public String getLastError() { return lastError; } public void setLastError(String v) { lastError = v; }
  public LocalDateTime getProcessedAt() { return processedAt; } public void setProcessedAt(LocalDateTime v) { processedAt = v; }
}
