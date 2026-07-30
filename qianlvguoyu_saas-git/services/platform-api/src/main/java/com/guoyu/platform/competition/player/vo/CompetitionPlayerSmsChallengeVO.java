package com.guoyu.platform.competition.player.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record CompetitionPlayerSmsChallengeVO(
    @JsonSerialize(using = ToStringSerializer.class) Long challengeId,
    LocalDateTime expiresAt) {
}
