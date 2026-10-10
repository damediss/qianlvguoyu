package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ExamEquipmentInfoSaveReq(
    @NotBlank String equipmentType,
    @NotBlank String assetNo,
    @NotNull @Positive Integer equipmentQuantity,
    Long siteId,
    String siteName,
    String equipmentModel,
    LocalDate calibrationValidUntil,
    String equipmentStatus,
    Long equipmentPhotoFileId,
    String equipmentPhotoFileName,
    String materialsDescription,
    String remark
) {
}
