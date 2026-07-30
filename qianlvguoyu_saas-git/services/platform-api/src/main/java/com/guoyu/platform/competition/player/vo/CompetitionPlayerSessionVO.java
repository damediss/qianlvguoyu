package com.guoyu.platform.competition.player.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record CompetitionPlayerSessionVO(
    @JsonSerialize(using = ToStringSerializer.class) Long playerId,
    @JsonSerialize(using = ToStringSerializer.class) Long tenantId,
    String token,
    String tokenType,
    long expiresIn) {
}
