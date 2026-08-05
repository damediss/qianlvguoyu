package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_practical_assignment")
public class CompetitionPracticalAssignmentEntity extends BaseEntity {
  private Long planId;
  private Long sessionId;
  private Long judgeGroupId;
  private Long rosterItemId;
  private Long contestantMemberId;
  private Long refereeMemberId;
  private String workstationCode;
  private String status;
  private LocalDateTime assignedAt;
  private LocalDateTime endedAt;
  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long value) { sessionId = value; }
  public Long getJudgeGroupId() { return judgeGroupId; }
  public void setJudgeGroupId(Long value) { judgeGroupId = value; }
  public Long getRosterItemId() { return rosterItemId; }
  public void setRosterItemId(Long value) { rosterItemId = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public Long getRefereeMemberId() { return refereeMemberId; }
  public void setRefereeMemberId(Long value) { refereeMemberId = value; }
  public String getWorkstationCode() { return workstationCode; }
  public void setWorkstationCode(String value) { workstationCode = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public LocalDateTime getAssignedAt() { return assignedAt; }
  public void setAssignedAt(LocalDateTime value) { assignedAt = value; }
  public LocalDateTime getEndedAt() { return endedAt; }
  public void setEndedAt(LocalDateTime value) { endedAt = value; }
}
