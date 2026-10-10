package com.guoyu.platform.competition.player.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record CompetitionPlayerEntryVO(
    @JsonSerialize(using = ToStringSerializer.class) Long tenantId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    String eventName,
    String eventStatus) {
}
