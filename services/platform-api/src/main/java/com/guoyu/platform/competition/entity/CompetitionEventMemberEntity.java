package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_event_member")
public class CompetitionEventMemberEntity extends BaseEntity {

  private Long eventId;
  private Long personId;
  private String roleType;
  private String status;
  private LocalDateTime joinedAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getPersonId() { return personId; }
  public void setPersonId(Long personId) { this.personId = personId; }
  public String getRoleType() { return roleType; }
  public void setRoleType(String roleType) { this.roleType = roleType; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getJoinedAt() { return joinedAt; }
  public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
