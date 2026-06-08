package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskEntity;
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
    String effectiveStatus = effectiveStatus(entity.getStatus(), entity.getCurrentPhase());
    return new CertificationTaskVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getTaskNo(),
        entity.getTaskName(),
        entity.getTaskManagerName(),
        entity.getCurrentPhase(),
        phaseName(entity.getCurrentPhase()),
        effectiveStatus,
        statusName(effectiveStatus),
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

  private static String phaseName(String phase) {
    return switch (phase == null ? "" : phase) {
      case "preparation" -> "任务准备阶段";
      case "before_exam" -> "考前准备阶段";
      case "theory_exam" -> "理论考试阶段";
      case "practical_exam" -> "实操考试阶段";
      case "archive" -> "归档总结阶段";
      default -> phase;
    };
  }

  private static String statusName(String status) {
    return switch (status == null ? "" : status) {
      case "draft" -> "草稿";
      case "running" -> "进行中";
      case "pending_review" -> "待审核";
      case "approved" -> "已通过";
      case "rejected" -> "已驳回";
      case "project_prepared" -> "认定准备已完成";
      case "preparation_completed" -> "认定筹备已完成";
      case "archive_completed" -> "认定归档已完成";
      case "completed" -> "已完成";
      default -> status;
    };
  }

  private static String effectiveStatus(String status, String currentPhase) {
    return status;
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
