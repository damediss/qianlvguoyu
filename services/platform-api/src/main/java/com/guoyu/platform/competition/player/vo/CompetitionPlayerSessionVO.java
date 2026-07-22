package com.guoyu.platform.competition.player.vo;

public record CompetitionPlayerSessionVO(Long playerId, Long tenantId, String token, String tokenType, long expiresIn) {
}
