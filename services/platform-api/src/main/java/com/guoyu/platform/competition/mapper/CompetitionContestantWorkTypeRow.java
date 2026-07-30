package com.guoyu.platform.competition.mapper;

public class CompetitionContestantWorkTypeRow {

  private Long profileId;
  private Long contestantMemberId;
  private Long registrationId;
  private Long eventWorkTypeId;
  private String workTypeMatchStatus;
  private String workTypeMatchNote;
  private String rosterWorkType;

  public Long getProfileId() { return profileId; }
  public void setProfileId(Long profileId) { this.profileId = profileId; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getWorkTypeMatchStatus() { return workTypeMatchStatus; }
  public void setWorkTypeMatchStatus(String workTypeMatchStatus) {
    this.workTypeMatchStatus = workTypeMatchStatus;
  }
  public String getWorkTypeMatchNote() { return workTypeMatchNote; }
  public void setWorkTypeMatchNote(String workTypeMatchNote) { this.workTypeMatchNote = workTypeMatchNote; }
  public String getRosterWorkType() { return rosterWorkType; }
  public void setRosterWorkType(String rosterWorkType) { this.rosterWorkType = rosterWorkType; }
}
