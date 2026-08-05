package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CompetitionPracticalSessionCreateReq(
    @NotNull Long planId,
    @NotBlank String sessionCode,
    @NotBlank String sessionName,
    @NotNull LocalDateTime startAt,
    @NotNull LocalDateTime endAt,
    Integer capacity,
    Long resourceId) { }
