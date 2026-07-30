package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;

public record PreparationAssessorGroupVO(
    Long id,
    Long taskId,
    String groupName,
    Long leaderAssessorId,
    String leaderAssessorName,
    String workTypeName,
    String status,
    String remark
) {
  public static PreparationAssessorGroupVO from(CertificationTaskAssessorGroupEntity entity, String leaderAssessorName) {
    return new PreparationAssessorGroupVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getGroupName(),
        entity.getLeaderAssessorId(),
        leaderAssessorName,
        entity.getWorkTypeName(),
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
