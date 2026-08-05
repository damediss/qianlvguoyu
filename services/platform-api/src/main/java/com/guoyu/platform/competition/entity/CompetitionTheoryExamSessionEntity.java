package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_theory_exam_session")
public class CompetitionTheoryExamSessionEntity extends BaseEntity {

  private Long scheduleVersionId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long paperSnapshotId;
  private String sessionCode;
  private String sessionName;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private Integer totalCapacity;
  private String status;
  private String executionStatus;
  private LocalDateTime openedAt;
  private LocalDateTime closedAt;

  public Long getScheduleVersionId() { return scheduleVersionId; }
  public void setScheduleVersionId(Long scheduleVersionId) { this.scheduleVersionId = scheduleVersionId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public String getSessionCode() { return sessionCode; }
  public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }
  public String getSessionName() { return sessionName; }
  public void setSessionName(String sessionName) { this.sessionName = sessionName; }
  public LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
  public LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
  public Integer getTotalCapacity() { return totalCapacity; }
  public void setTotalCapacity(Integer totalCapacity) { this.totalCapacity = totalCapacity; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getExecutionStatus() { return executionStatus; }
  public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }
  public LocalDateTime getOpenedAt() { return openedAt; }
  public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
  public LocalDateTime getClosedAt() { return closedAt; }
  public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
