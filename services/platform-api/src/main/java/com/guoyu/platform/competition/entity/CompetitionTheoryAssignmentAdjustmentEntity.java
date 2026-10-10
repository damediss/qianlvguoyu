package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_theory_assignment_adjustment")
public class CompetitionTheoryAssignmentAdjustmentEntity extends BaseEntity {

  private Long scheduleVersionId;
  private Long assignmentId;
  private Long contestantProfileId;
  private Long fromSessionId;
  private Long fromSessionRoomId;
  private String fromSeatNo;
  private Long toSessionId;
  private Long toSessionRoomId;
  private String toSeatNo;
  private String reason;
  private Long adjustedBy;
  private LocalDateTime adjustedAt;

  public Long getScheduleVersionId() { return scheduleVersionId; }
  public void setScheduleVersionId(Long scheduleVersionId) { this.scheduleVersionId = scheduleVersionId; }
  public Long getAssignmentId() { return assignmentId; }
  public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) { this.contestantProfileId = contestantProfileId; }
  public Long getFromSessionId() { return fromSessionId; }
  public void setFromSessionId(Long fromSessionId) { this.fromSessionId = fromSessionId; }
  public Long getFromSessionRoomId() { return fromSessionRoomId; }
  public void setFromSessionRoomId(Long fromSessionRoomId) { this.fromSessionRoomId = fromSessionRoomId; }
  public String getFromSeatNo() { return fromSeatNo; }
  public void setFromSeatNo(String fromSeatNo) { this.fromSeatNo = fromSeatNo; }
  public Long getToSessionId() { return toSessionId; }
  public void setToSessionId(Long toSessionId) { this.toSessionId = toSessionId; }
  public Long getToSessionRoomId() { return toSessionRoomId; }
  public void setToSessionRoomId(Long toSessionRoomId) { this.toSessionRoomId = toSessionRoomId; }
  public String getToSeatNo() { return toSeatNo; }
  public void setToSeatNo(String toSeatNo) { this.toSeatNo = toSeatNo; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Long getAdjustedBy() { return adjustedBy; }
  public void setAdjustedBy(Long adjustedBy) { this.adjustedBy = adjustedBy; }
  public LocalDateTime getAdjustedAt() { return adjustedAt; }
  public void setAdjustedAt(LocalDateTime adjustedAt) { this.adjustedAt = adjustedAt; }
}
