package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_referee_group")
public class CompetitionRefereeGroupEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Integer groupNo;
  private String groupName;
  private Long leaderRefereeMemberId;
  private Long leaderRefereeProfileId;
  private String status;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) {
    this.eventWorkTypeLevelId = eventWorkTypeLevelId;
  }
  public Integer getGroupNo() { return groupNo; }
  public void setGroupNo(Integer groupNo) { this.groupNo = groupNo; }
  public String getGroupName() { return groupName; }
  public void setGroupName(String groupName) { this.groupName = groupName; }
  public Long getLeaderRefereeMemberId() { return leaderRefereeMemberId; }
  public void setLeaderRefereeMemberId(Long leaderRefereeMemberId) {
    this.leaderRefereeMemberId = leaderRefereeMemberId;
  }
  public Long getLeaderRefereeProfileId() { return leaderRefereeProfileId; }
  public void setLeaderRefereeProfileId(Long leaderRefereeProfileId) {
    this.leaderRefereeProfileId = leaderRefereeProfileId;
  }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
