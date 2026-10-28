package com.guoyu.platform.competition.prepractice.mapper.row;

public class CompetitionPrePracticeContestantRow {
  private Long profileId;
  private Long registrationId;
  private Long playerId;
  private String contestantNo;
  private String contestantName;
  private Long orgId;
  private String orgName;

  public Long getProfileId() { return profileId; }
  public void setProfileId(Long profileId) { this.profileId = profileId; }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getContestantNo() { return contestantNo; }
  public void setContestantNo(String contestantNo) { this.contestantNo = contestantNo; }
  public String getContestantName() { return contestantName; }
  public void setContestantName(String contestantName) { this.contestantName = contestantName; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
}
