package com.guoyu.platform.competition.mapper;

/** Contestant in an event scope, keyed by the player ID-card lookup hash. */
public class CompetitionSubjectScoreContestantRow {
  private Long contestantMemberId;
  private String idCardLookupHash;

  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public String getIdCardLookupHash() { return idCardLookupHash; }
  public void setIdCardLookupHash(String idCardLookupHash) {
    this.idCardLookupHash = idCardLookupHash;
  }
}
