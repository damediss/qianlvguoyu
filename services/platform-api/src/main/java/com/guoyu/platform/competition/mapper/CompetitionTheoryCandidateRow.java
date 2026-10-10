package com.guoyu.platform.competition.mapper;

public class CompetitionTheoryCandidateRow {

  private Long contestantProfileId;
  private Long contestantMemberId;
  private Long registrationId;
  private Long playerId;
  private String contestantNo;
  private String contestantName;
  private String orgName;

  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) {
    this.contestantProfileId = contestantProfileId;
  }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getContestantNo() { return contestantNo; }
  public void setContestantNo(String contestantNo) { this.contestantNo = contestantNo; }
  public String getContestantName() { return contestantName; }
  public void setContestantName(String contestantName) { this.contestantName = contestantName; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
}
