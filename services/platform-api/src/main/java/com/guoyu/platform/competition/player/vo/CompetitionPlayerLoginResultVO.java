package com.guoyu.platform.competition.player.vo;

/** Result of a competition-player credential check. A session is only present for active players. */
public record CompetitionPlayerLoginResultVO(String status, CompetitionPlayerSessionVO session) {

  public static CompetitionPlayerLoginResultVO active(CompetitionPlayerSessionVO session) {
    return new CompetitionPlayerLoginResultVO("active", session);
  }

  public static CompetitionPlayerLoginResultVO pendingActivation() {
    return new CompetitionPlayerLoginResultVO("pending_activation", null);
  }
}
