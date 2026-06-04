package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskEquipmentEntity;

public record PreparationTaskEquipmentVO(
    Long id,
    Long taskId,
    Long equipmentId,
    String equipmentName,
    String equipmentNo,
    Integer equipmentQuantity,
    String status,
    String remark
) {
  public static PreparationTaskEquipmentVO from(CertificationTaskEquipmentEntity entity) {
    return new PreparationTaskEquipmentVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getEquipmentId(),
        entity.getEquipmentName(),
        entity.getEquipmentNo(),
        entity.getEquipmentQuantity(),
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
