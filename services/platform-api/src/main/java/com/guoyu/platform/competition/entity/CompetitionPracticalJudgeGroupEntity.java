package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_practical_judge_group")
public class CompetitionPracticalJudgeGroupEntity extends BaseEntity {
  private Long sessionId;
  private Long planId;
  private Integer groupNo;
  private String groupName;
  private Integer officialCount;
  private String status;
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long value) { sessionId = value; }
  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Integer getGroupNo() { return groupNo; }
  public void setGroupNo(Integer value) { groupNo = value; }
  public String getGroupName() { return groupName; }
  public void setGroupName(String value) { groupName = value; }
  public Integer getOfficialCount() { return officialCount; }
  public void setOfficialCount(Integer value) { officialCount = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
}
