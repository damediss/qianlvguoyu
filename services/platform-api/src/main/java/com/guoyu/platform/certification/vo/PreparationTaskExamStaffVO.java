package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskExamStaffEntity;

public record PreparationTaskExamStaffVO(
    Long id,
    Long taskId,
    Long staffId,
    String staffName,
    String phone,
    String maskedPhone,
    String status,
    String taskRole,
    String roleName,
    Boolean isLeader,
    String remark
) {
  public static PreparationTaskExamStaffVO from(CertificationTaskExamStaffEntity entity) {
    return new PreparationTaskExamStaffVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getStaffId(),
        entity.getStaffName(),
        entity.getPhone(),
        entity.getMaskedPhone(),
        entity.getStatus(),
        entity.getTaskRole(),
        entity.getRoleName(),
        entity.getIsLeader() != null && entity.getIsLeader() == 1,
        entity.getRemark()
    );
  }
}
