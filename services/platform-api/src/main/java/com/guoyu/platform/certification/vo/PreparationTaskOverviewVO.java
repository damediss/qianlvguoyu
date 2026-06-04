package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PreparationTaskOverviewVO(
    Long id,
    String taskNo,
    String taskName,
    String batchName,
    String workTypeName,
    String levelName,
    LocalDate planStartDate,
    LocalDate planEndDate,
    String preparationStatus,
    long candidateCount,
    int documentCompletionRate,
    String materialStatus,
    long examSiteCount,
    long examStaffCount,
    long assessorCount,
    LocalDateTime lastUpdatedAt
) {
  public static PreparationTaskOverviewVO from(
      CertificationTaskEntity task,
      List<CertificationTaskScopeEntity> scopes,
      long candidateCount,
      int documentCompletionRate,
      String materialStatus,
      long examSiteCount,
      long examStaffCount,
      long assessorCount
  ) {
    return new PreparationTaskOverviewVO(
        task.getId(),
        task.getTaskNo(),
        task.getTaskName(),
        task.getSourceName(),
        summarizeWorkTypes(scopes),
        summarizeLevelNames(scopes),
        task.getPlanStartDate(),
        task.getPlanEndDate(),
        task.getStatus(),
        candidateCount,
        documentCompletionRate,
        materialStatus,
        examSiteCount,
        examStaffCount,
        assessorCount,
        task.getLastBusinessUpdatedAt()
    );
  }

  private static String summarizeWorkTypes(List<CertificationTaskScopeEntity> scopes) {
    return safeScopes(scopes).stream()
        .map(CertificationTaskScopeEntity::getWorkTypeName)
        .filter(value -> value != null && !value.isBlank())
        .distinct()
        .collect(Collectors.joining("、"));
  }

  private static String summarizeLevelNames(List<CertificationTaskScopeEntity> scopes) {
    return safeScopes(scopes).stream()
        .map(CertificationTaskScopeEntity::getLevelName)
        .filter(value -> value != null && !value.isBlank())
        .distinct()
        .collect(Collectors.joining("、"));
  }

  private static List<CertificationTaskScopeEntity> safeScopes(List<CertificationTaskScopeEntity> scopes) {
    return scopes == null ? List.of() : scopes;
  }
}
