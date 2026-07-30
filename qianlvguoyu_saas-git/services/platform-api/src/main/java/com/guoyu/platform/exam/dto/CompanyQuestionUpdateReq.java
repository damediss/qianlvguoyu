package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CompanyQuestionUpdateReq(
    @Size(max = 50) String type,
    String content,
    String answer,
    Long orgId,
    String orgName,
    Integer difficulty,
    BigDecimal score,
    String resolve,
    String keyword,
    Integer status,
    Integer sortNumber
) {
}
