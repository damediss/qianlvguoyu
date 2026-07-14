package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CertVerifyReq(
    @NotEmpty(message = "请至少选择一名人员") List<Long> peopleIds
) {}
