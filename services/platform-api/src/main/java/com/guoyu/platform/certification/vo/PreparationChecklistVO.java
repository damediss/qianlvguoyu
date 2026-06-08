package com.guoyu.platform.certification.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PreparationChecklistVO(
    Long taskId,
    String taskName,
    int totalCount,
    int checkedCount,
    boolean allChecked,
    Long confirmerTaskStaffId,
    String confirmerName,
    LocalDateTime confirmedAt,
    List<PreparationChecklistItemVO> items
) {
}
