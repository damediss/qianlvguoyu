package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationAssessorTaskScoreEntity;
import java.math.BigDecimal;

public record PreparationScoreVO(
    Long id,
    Integer stepNo,
    String stepName,
    BigDecimal maxScore,
    BigDecimal score,
    String remark
) {
  public static PreparationScoreVO from(CertificationAssessorTaskScoreEntity entity) {
    return new PreparationScoreVO(
        entity.getId(),
        entity.getStepNo(),
        entity.getStepName(),
        entity.getMaxScore(),
        entity.getScore(),
        entity.getRemark()
    );
  }
}
