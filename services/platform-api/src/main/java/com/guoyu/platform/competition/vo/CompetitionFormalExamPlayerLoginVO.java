package com.guoyu.platform.competition.vo;

public record CompetitionFormalExamPlayerLoginVO(
    String playerId,
    String tenantId,
    String accessToken,
    String tokenType,
    long expiresIn,
    String accessSessionId,
    Integer generation,
    String deviceId,
    String examSessionId,
    String candidateExamId) {

  public CompetitionFormalExamPlayerLoginVO(
      String playerId, String tenantId, String accessToken, String tokenType, long expiresIn) {
    this(playerId, tenantId, accessToken, tokenType, expiresIn, null, null, null, null, null);
  }
}
