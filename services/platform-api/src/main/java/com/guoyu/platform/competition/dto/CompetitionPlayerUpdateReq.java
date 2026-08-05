package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionPlayerUpdateReq(
    @NotNull Long orgId,
    @NotNull Long eventWorkTypeId,
    @Size(max = 64) String employeeNo,
    @Size(max = 128) String name,
    @Size(max = 32) String idCardNo,
    @Size(max = 32) String mobile,
    @Size(max = 32) String level) {
}
