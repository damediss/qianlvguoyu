package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.service.CertificationTaskPhases;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record CertificationTaskVO(
    String id,
    String taskNo,
    String taskName,
    String taskManagerName,
    String currentPhase,
    String currentPhaseName,
    String status,
    String statusName,
    String sourceType,
    String sourceName,
    String workTypeName,
    String levelCode,
    String levelName,
    List<CertificationTaskScopeVO> scopes,
    Integer plannedPeopleCount,
    LocalDate planStartDate,
    LocalDate planEndDate,
    LocalDate examDate,
    Integer documentCompletedCount,
    Integer documentTotalCount,
    Integer phaseCompletedCount,
    Integer phaseTotalCount,
    LocalDateTime lastBusinessUpdatedAt
) {

  public static CertificationTaskVO from(CertificationTaskEntity entity, List<CertificationTaskScopeVO> scopes) {
    List<CertificationTaskScopeVO> safeScopes = scopes == null ? List.of() : scopes;
    String currentPhase = normalizePhase(entity.getCurrentPhase());
    String status = normalizeStatus(entity.getStatus());
    return new CertificationTaskVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getTaskNo(),
        entity.getTaskName(),
        entity.getTaskManagerName(),
        currentPhase,
        CertificationTaskPhases.phaseName(currentPhase),
        status,
        statusName(status),
        entity.getSourceType(),
        sourceName(entity.getSourceType(), entity.getSourceName()),
        summarizeWorkTypes(safeScopes),
        summarizeLevelCodes(safeScopes),
        summarizeLevelNames(safeScopes),
        safeScopes,
        entity.getPlannedPeopleCount(),
        entity.getPlanStartDate(),
        entity.getPlanEndDate(),
        entity.getExamDate(),
        entity.getDocumentCompletedCount(),
        entity.getDocumentTotalCount(),
        entity.getPhaseCompletedCount(),
        entity.getPhaseTotalCount(),
        entity.getLastBusinessUpdatedAt()
    );
  }

  private static String summarizeWorkTypes(List<CertificationTaskScopeVO> scopes) {
    return scopes.stream()
        .map(CertificationTaskScopeVO::workTypeName)
        .filter(value -> value != null && !value.isBlank())
        .distinct()
        .collect(Collectors.joining("、"));
  }

  private static String summarizeLevelCodes(List<CertificationTaskScopeVO> scopes) {
    return scopes.stream()
        .map(CertificationTaskScopeVO::levelCode)
        .filter(value -> value != null && !value.isBlank())
        .distinct()
        .collect(Collectors.joining(","));
  }

  private static String summarizeLevelNames(List<CertificationTaskScopeVO> scopes) {
    return scopes.stream()
        .map(CertificationTaskScopeVO::levelName)
        .filter(value -> value != null && !value.isBlank())
        .distinct()
        .collect(Collectors.joining("、"));
  }

  // 把历史阶段编码（project_creation 等）规范化为新的 4 阶段编码
  private static String normalizePhase(String phase) {
    if (phase == null) {
      return CertificationTaskPhases.TASK_CREATION;
    }
    return switch (phase) {
      case "project_creation" -> CertificationTaskPhases.TASK_CREATION;
      case CertificationTaskPhases.TASK_CREATION,
           CertificationTaskPhases.PREPARATION,
           CertificationTaskPhases.ARCHIVE,
           CertificationTaskPhases.CERTIFICATION_COMPLETED -> phase;
      default -> CertificationTaskPhases.TASK_CREATION;
    };
  }

  // 把历史 status 取值规范化为当前状态枚举
  private static String normalizeStatus(String status) {
    if (status == null) {
      return "in_progress";
    }
    return switch (status) {
      case "pending_complete", "in_progress", "pending_review", "rejected", "completed" -> status;
      case "approved" -> "completed";
      case "running", "draft" -> "in_progress";
      case "archive_completed", "certification_completed" -> "completed";
      case "preparation_completed", "project_prepared",
           "preparation", "archive", "project_creation", "task_creation" -> "in_progress";
      default -> "in_progress";
    };
  }

  private static String statusName(String status) {
    return switch (status == null ? "" : status) {
      case "pending_complete" -> "待补充完整";
      case "in_progress" -> "进行中";
      case "pending_review" -> "待审核";
      case "rejected" -> "已驳回";
      case "completed" -> "已完成";
      default -> status;
    };
  }

  private static String sourceName(String sourceType, String sourceName) {
    if (sourceName != null && !sourceName.isBlank()) {
      return sourceName;
    }
    return switch (sourceType == null ? "" : sourceType) {
      case "order" -> "任务订单";
      case "manual" -> "手动新增";
      default -> sourceType;
    };
  }
}
