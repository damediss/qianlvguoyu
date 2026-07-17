package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionNoticeUpdateReq(
    @NotBlank @Size(max = 256) String title,
    @NotBlank @Size(max = 16000) String content,
    @Min(1) Integer version
) {
}
