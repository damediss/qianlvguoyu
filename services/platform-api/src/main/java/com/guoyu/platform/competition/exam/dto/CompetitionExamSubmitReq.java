package com.guoyu.platform.competition.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionExamSubmitReq(@NotBlank @Size(max = 64) String requestId) {
}
