package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionSimulationSyncRowVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long batchId,
    Integer rowNo,
    String externalParticipantId,
    String externalSessionId,
    String playerName,
    String idCardMasked,
    @JsonSerialize(using = ToStringSerializer.class) Long contestantMemberId,
    BigDecimal rawScore,
    String moduleTitle,
    String appDisplayName,
    Integer durationSeconds,
    LocalDateTime achievedAt,
    String validationStatus,
    String errorMessage,
    @JsonSerialize(using = ToStringSerializer.class) Long scoreSourceId,
    JsonNode detail) {}
