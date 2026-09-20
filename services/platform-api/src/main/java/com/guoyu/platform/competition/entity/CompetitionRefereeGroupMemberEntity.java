package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_referee_group_member")
public class CompetitionRefereeGroupMemberEntity extends BaseEntity {

  private Long groupId;
  private Long refereeProfileId;
  private Long refereeMemberId;
  private String memberRole;
  private String status;

  public Long getGroupId() { return groupId; }
  public void setGroupId(Long groupId) { this.groupId = groupId; }
  public Long getRefereeProfileId() { return refereeProfileId; }
  public void setRefereeProfileId(Long refereeProfileId) { this.refereeProfileId = refereeProfileId; }
  public Long getRefereeMemberId() { return refereeMemberId; }
  public void setRefereeMemberId(Long refereeMemberId) { this.refereeMemberId = refereeMemberId; }
  public String getMemberRole() { return memberRole; }
  public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
