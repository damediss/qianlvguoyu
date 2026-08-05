package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_resource_booking")
public class CompetitionStageResourceBookingEntity extends BaseEntity {
  private Long planId;
  private Long eventId;
  private Long stageId;
  private String resourceType;
  private Long resourceId;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String status;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public String getResourceType() { return resourceType; }
  public void setResourceType(String value) { resourceType = value; }
  public Long getResourceId() { return resourceId; }
  public void setResourceId(Long value) { resourceId = value; }
  public LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(LocalDateTime value) { startAt = value; }
  public LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(LocalDateTime value) { endAt = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}
