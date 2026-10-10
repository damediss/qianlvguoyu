package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CompetitionRefereeQualificationReq(
    @NotNull Long workTypeId,
    @NotBlank @Pattern(regexp = "[1-5]", message = "起始工种等级必须是1至5") String levelFromCode,
    @NotBlank @Pattern(regexp = "[1-5]", message = "结束工种等级必须是1至5") String levelToCode) {
}
