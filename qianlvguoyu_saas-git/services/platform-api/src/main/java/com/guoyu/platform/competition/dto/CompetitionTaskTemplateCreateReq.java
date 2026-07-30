package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionTaskTemplateCreateReq(
    @NotBlank @Size(max = 32) String taskType,
    @NotBlank @Size(max = 128) String title,
    @Size(max = 2000) String description,
    @Size(max = 2000) String completionRule,
    @Min(0) @Max(1) Integer requiresReview,
    @Size(max = 512) String deadlineRule,
    @Size(max = 512) String allowedFileTypes,
    @Min(0) Integer sortOrder
) {
}
