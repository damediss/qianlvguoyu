package com.guoyu.platform.competition.vo;

import java.util.ArrayList;
import java.util.List;

public class CompetitionContestantWorkTypeReconciliationVO {

  private Long profileId;
  private Long contestantMemberId;
  private Long registrationId;
  private String rosterWorkType;
  private Long eventWorkTypeId;
  private String matchStatus;
  private boolean confirmed;
  private String note;
  private List<CompetitionWorkTypeCandidateVO> candidates = new ArrayList<>();

  public Long getProfileId() { return profileId; }
  public void setProfileId(Long profileId) { this.profileId = profileId; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public String getRosterWorkType() { return rosterWorkType; }
  public void setRosterWorkType(String rosterWorkType) { this.rosterWorkType = rosterWorkType; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getMatchStatus() { return matchStatus; }
  public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }
  public boolean isConfirmed() { return confirmed; }
  public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
  public List<CompetitionWorkTypeCandidateVO> getCandidates() { return candidates; }
  public void setCandidates(List<CompetitionWorkTypeCandidateVO> candidates) {
    this.candidates = candidates == null ? new ArrayList<>() : new ArrayList<>(candidates);
  }
}
