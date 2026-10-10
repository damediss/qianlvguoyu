package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_practical_judge_member")
public class CompetitionPracticalJudgeMemberEntity extends BaseEntity {
  private Long judgeGroupId;
  private Long sessionId;
  private Long refereeMemberId;
  private String memberRole;
  private String status;
  public Long getJudgeGroupId() { return judgeGroupId; }
  public void setJudgeGroupId(Long value) { judgeGroupId = value; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long value) { sessionId = value; }
  public Long getRefereeMemberId() { return refereeMemberId; }
  public void setRefereeMemberId(Long value) { refereeMemberId = value; }
  public String getMemberRole() { return memberRole; }
  public void setMemberRole(String value) { memberRole = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}
