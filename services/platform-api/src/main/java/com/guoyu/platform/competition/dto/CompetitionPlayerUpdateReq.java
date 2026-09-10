package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public record CompetitionPlayerUpdateReq(
    @NotNull Long orgId,
    Long eventWorkTypeId,
    @Size(max = 128) String name,
    @Size(max = 32) String idCardNo,
    @Size(max = 32) String mobile,
    @Size(max = 32) String level,
    @Valid List<CompetitionPlayerWorkTypeReq> workTypes) {
}
