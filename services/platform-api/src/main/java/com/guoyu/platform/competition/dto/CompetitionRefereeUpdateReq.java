package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionRefereeUpdateReq(
    @NotEmpty @Valid List<CompetitionRefereeQualificationReq> qualifications,
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 32) String idCardNo,
    @NotBlank @Size(max = 32) String mobile) {
}
