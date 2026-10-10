package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CompanyQuestionCreateReq(
    @NotBlank @Size(max = 50) String type,
    String content,
    String answer,
    @NotNull Long orgId,
    @NotBlank String orgName,
    Integer difficulty,
    BigDecimal score,
    String resolve,
    String keyword,
    Integer status,
    Integer sortNumber
) {
}
