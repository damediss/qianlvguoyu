package com.guoyu.platform.competition.player.vo;

import java.time.LocalDateTime;

public record CompetitionPlayerSmsChallengeVO(Long challengeId, LocalDateTime expiresAt) {
}
