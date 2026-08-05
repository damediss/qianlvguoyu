package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_practical_session")
public class CompetitionPracticalSessionEntity extends BaseEntity {
  private Long planId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String sessionCode;
  private String sessionName;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private Integer capacity;
  private Long resourceId;
  private String status;
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
  public String getSessionCode() { return sessionCode; }
  public void setSessionCode(String value) { sessionCode = value; }
  public String getSessionName() { return sessionName; }
  public void setSessionName(String value) { sessionName = value; }
  public LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(LocalDateTime value) { startAt = value; }
  public LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(LocalDateTime value) { endAt = value; }
  public Integer getCapacity() { return capacity; }
  public void setCapacity(Integer value) { capacity = value; }
  public Long getResourceId() { return resourceId; }
  public void setResourceId(Long value) { resourceId = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}
