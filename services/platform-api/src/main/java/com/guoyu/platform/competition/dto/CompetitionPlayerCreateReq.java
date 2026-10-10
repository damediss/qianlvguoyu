package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public record CompetitionPlayerCreateReq(
    @NotNull Long eventId,
    @NotNull Long orgId,
    Long eventWorkTypeId,
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 32) String idCardNo,
    @NotBlank @Size(max = 32) String mobile,
    @Size(max = 32) String level,
    @Valid List<CompetitionPlayerWorkTypeReq> workTypes) {
}
