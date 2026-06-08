package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskEquipmentEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamEquipmentEntity;

public record PreparationTaskEquipmentVO(
    Long id,
    Long taskId,
    Long equipmentId,
    String equipmentName,
    String equipmentNo,
    Integer equipmentQuantity,
    String status,
    String remark,
    Long equipmentPhotoFileId,
    String equipmentPhotoFileName
) {
  public static PreparationTaskEquipmentVO from(CertificationTaskEquipmentEntity entity) {
    return from(entity, null);
  }

  public static PreparationTaskEquipmentVO from(
      CertificationTaskEquipmentEntity entity,
      CertificationInfoExamEquipmentEntity equipment
  ) {
    return new PreparationTaskEquipmentVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getEquipmentId(),
        entity.getEquipmentName(),
        entity.getEquipmentNo(),
        entity.getEquipmentQuantity(),
        entity.getStatus(),
        entity.getRemark(),
        equipment != null ? equipment.getEquipmentPhotoFileId() : null,
        equipment != null ? equipment.getEquipmentPhotoFileName() : null
    );
  }
}
