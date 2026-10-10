package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskChecklistItemEntity;
import java.time.LocalDateTime;

public record PreparationChecklistItemVO(
    Long id,
    Long taskId,
    Integer itemNo,
    String itemName,
    String quantityText,
    boolean checked,
    Long confirmerTaskStaffId,
    String confirmerName,
    LocalDateTime confirmedAt,
    String remark
) {
  public static PreparationChecklistItemVO from(CertificationTaskChecklistItemEntity entity) {
    return new PreparationChecklistItemVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getItemNo(),
        entity.getItemName(),
        entity.getQuantityText(),
        entity.getChecked() != null && entity.getChecked() == 1,
        entity.getConfirmerTaskStaffId(),
        entity.getConfirmerName(),
        entity.getConfirmedAt(),
        entity.getRemark()
    );
  }
}
