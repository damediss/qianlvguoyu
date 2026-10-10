package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PreparationChecklistUpdateReq(
    @NotNull List<Long> checkedItemIds,
    Long confirmerTaskStaffId,
    String remark
) {
}
