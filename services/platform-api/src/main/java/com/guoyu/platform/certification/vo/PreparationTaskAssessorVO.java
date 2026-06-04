package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import java.time.LocalDate;

public record PreparationTaskAssessorVO(
    Long id,
    Long taskId,
    Long groupId,
    Long assessorId,
    String assessorName,
    String certificateNo,
    String taskRole,
    String roleName,
    String workTypeScope,
    LocalDate validUntil,
    String workStatus
) {
  public static PreparationTaskAssessorVO from(CertificationTaskAssessorEntity entity) {
    return new PreparationTaskAssessorVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getGroupId(),
        entity.getAssessorId(),
        entity.getAssessorName(),
        entity.getCertificateNo(),
        entity.getTaskRole(),
        entity.getRoleName(),
        entity.getWorkTypeScope(),
        entity.getValidUntil(),
        entity.getWorkStatus()
    );
  }
}
