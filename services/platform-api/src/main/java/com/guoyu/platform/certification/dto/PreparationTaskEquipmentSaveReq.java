package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotNull;

public record PreparationTaskEquipmentSaveReq(
    @NotNull Long equipmentId,
    String remark
) {
}
