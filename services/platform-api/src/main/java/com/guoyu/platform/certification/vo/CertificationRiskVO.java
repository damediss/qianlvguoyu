package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationRiskEntity;
import java.time.LocalDate;

public record CertificationRiskVO(
    String id,
    String riskNo,
    String riskDescription,
    String probabilityCode,
    String impactCode,
    String riskLevelCode,
    Integer riskLevelScore,
    String responsePlan,
    String ownerMemberId,
    String ownerName,
    String status,
    LocalDate expectedCloseDate,
    String remark
) {

  public static CertificationRiskVO from(CertificationRiskEntity entity) {
    return new CertificationRiskVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getRiskNo(),
        entity.getRiskDescription(),
        entity.getProbabilityCode(),
        entity.getImpactCode(),
        entity.getRiskLevelCode(),
        entity.getRiskLevelScore(),
        entity.getResponsePlan(),
        entity.getOwnerMemberId() == null ? null : String.valueOf(entity.getOwnerMemberId()),
        entity.getOwnerName(),
        entity.getStatus(),
        entity.getExpectedCloseDate(),
        entity.getRemark()
    );
  }
}
