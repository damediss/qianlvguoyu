package com.guoyu.platform.competition.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionExamLoginReq(
    @NotBlank @Size(max = 32) String account,
    @NotBlank @Size(min = 6, max = 64) String password) {
}
