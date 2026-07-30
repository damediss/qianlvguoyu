package com.guoyu.platform.certification.dto;

import java.math.BigDecimal;

public record PreparationScoreReq(
    Integer stepNo,
    String stepName,
    BigDecimal maxScore,
    BigDecimal score,
    String remark
) {
}
