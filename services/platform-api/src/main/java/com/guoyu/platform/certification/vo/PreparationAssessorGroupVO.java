package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import java.util.List;

public record PreparationAssessorGroupVO(
    Long id,
    Long taskId,
    String groupName,
    Long leaderAssessorId,
    String leaderAssessorName,
    String workTypeName,
    List<AssessorGroupWorktypeItem> workTypeScopes,
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
        List.of(),
        entity.getStatus(),
        entity.getRemark()
    );
  }

  public static PreparationAssessorGroupVO from(
      CertificationTaskAssessorGroupEntity entity,
      String leaderAssessorName,
      List<AssessorGroupWorktypeItem> workTypeScopes
  ) {
    return new PreparationAssessorGroupVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getGroupName(),
        entity.getLeaderAssessorId(),
        leaderAssessorName,
        entity.getWorkTypeName(),
        workTypeScopes == null ? List.of() : workTypeScopes,
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
