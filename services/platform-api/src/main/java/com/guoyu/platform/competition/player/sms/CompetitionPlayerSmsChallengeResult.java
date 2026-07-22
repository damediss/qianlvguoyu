package com.guoyu.platform.competition.player.sms;

import java.time.LocalDateTime;

public record CompetitionPlayerSmsChallengeResult(Long challengeId, LocalDateTime expiresAt) {
}
