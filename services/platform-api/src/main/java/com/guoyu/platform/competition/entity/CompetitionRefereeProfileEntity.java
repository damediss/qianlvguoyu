package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_referee_profile")
public class CompetitionRefereeProfileEntity extends BaseEntity {

  private Long memberId;
  private Long personId;
  private Long userId;
  private String accountUsername;
  private String refereeName;
  private String status;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String professionalField;
  private String refereeLevel;

  public Long getMemberId() { return memberId; }
  public void setMemberId(Long memberId) { this.memberId = memberId; }
  public Long getPersonId() { return personId; }
  public void setPersonId(Long personId) { this.personId = personId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getAccountUsername() { return accountUsername; }
  public void setAccountUsername(String accountUsername) { this.accountUsername = accountUsername; }
  public String getRefereeName() { return refereeName; }
  public void setRefereeName(String refereeName) { this.refereeName = refereeName; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) {
    this.eventWorkTypeLevelId = eventWorkTypeLevelId;
  }
  public String getProfessionalField() { return professionalField; }
  public void setProfessionalField(String professionalField) { this.professionalField = professionalField; }
  public String getRefereeLevel() { return refereeLevel; }
  public void setRefereeLevel(String refereeLevel) { this.refereeLevel = refereeLevel; }
}
