package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationWbsItemEntity;
import java.math.BigDecimal;

public record CertificationWbsItemVO(
    String id,
    String wbsCode,
    String categoryCode,
    String categoryName,
    String itemName,
    String activityDescription,
    BigDecimal estimateHours,
    BigDecimal manpowerCount,
    String manpowerRoleCode,
    String responsibleMemberId,
    String resourceInput,
    BigDecimal costAmount,
    Integer durationDays,
    String predecessorId,
    Integer progressPercent,
    String status
) {

  public static CertificationWbsItemVO from(CertificationWbsItemEntity entity) {
    return new CertificationWbsItemVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getWbsCode(),
        entity.getCategoryCode(),
        entity.getCategoryName(),
        entity.getItemName(),
        entity.getActivityDescription(),
        entity.getEstimateHours(),
        entity.getManpowerCount(),
        entity.getManpowerRoleCode(),
        entity.getResponsibleMemberId() == null ? null : String.valueOf(entity.getResponsibleMemberId()),
        entity.getResourceInput(),
        entity.getCostAmount(),
        entity.getDurationDays(),
        entity.getPredecessorId() == null ? null : String.valueOf(entity.getPredecessorId()),
        entity.getProgressPercent(),
        entity.getStatus()
    );
  }
}
