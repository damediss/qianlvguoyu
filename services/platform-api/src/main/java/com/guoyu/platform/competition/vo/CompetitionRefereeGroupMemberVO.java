package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class CompetitionRefereeGroupMemberVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long refereeMemberId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long refereeProfileId;
  private String refereeName;
  private String memberRole;
  private String status;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getRefereeMemberId() { return refereeMemberId; }
  public void setRefereeMemberId(Long refereeMemberId) { this.refereeMemberId = refereeMemberId; }
  public Long getRefereeProfileId() { return refereeProfileId; }
  public void setRefereeProfileId(Long refereeProfileId) { this.refereeProfileId = refereeProfileId; }
  public String getRefereeName() { return refereeName; }
  public void setRefereeName(String refereeName) { this.refereeName = refereeName; }
  public String getMemberRole() { return memberRole; }
  public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
