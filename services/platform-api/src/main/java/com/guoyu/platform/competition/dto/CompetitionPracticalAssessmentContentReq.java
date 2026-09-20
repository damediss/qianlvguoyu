package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** A rich-text assessment content group and its scoring elements. */
public record CompetitionPracticalAssessmentContentReq(
    @NotNull Integer sortOrder,
    @NotBlank String contentHtml,
    @Valid List<@Valid CompetitionPracticalAssessmentElementReq> elements
) {
}
