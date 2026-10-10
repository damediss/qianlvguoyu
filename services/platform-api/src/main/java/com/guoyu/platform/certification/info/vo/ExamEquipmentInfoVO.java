package com.guoyu.platform.certification.info.vo;

import java.time.LocalDate;

public record ExamEquipmentInfoVO(
    Long id,
    String equipmentType,
    String assetNo,
    Integer equipmentQuantity,
    Long siteId,
    String siteName,
    String equipmentModel,
    LocalDate calibrationValidUntil,
    String auditStatus,
    String equipmentStatus,
    Long equipmentPhotoFileId,
    String equipmentPhotoFileName,
    String materialsDescription,
    String remark
) {
}
