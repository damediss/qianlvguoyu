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
    String remark,
    int memberCount
) {
  public static PreparationAssessorGroupVO from(CertificationTaskAssessorGroupEntity entity, String leaderAssessorName) {
    return from(entity, leaderAssessorName, List.of(), 0);
  }

  public static PreparationAssessorGroupVO from(
      CertificationTaskAssessorGroupEntity entity,
      String leaderAssessorName,
      List<AssessorGroupWorktypeItem> workTypeScopes
  ) {
    return from(entity, leaderAssessorName, workTypeScopes, 0);
  }

  public static PreparationAssessorGroupVO from(
      CertificationTaskAssessorGroupEntity entity,
      String leaderAssessorName,
      List<AssessorGroupWorktypeItem> workTypeScopes,
      int memberCount
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
        entity.getRemark(),
        memberCount
    );
  }
}
