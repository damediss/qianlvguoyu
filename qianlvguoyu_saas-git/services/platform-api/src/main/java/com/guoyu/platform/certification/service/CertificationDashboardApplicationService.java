package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.dto.CertificationDashboardQueryReq;
import com.guoyu.platform.certification.entity.CertificationArchiveImportBatchEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveImportErrorEntity;
import com.guoyu.platform.certification.entity.CertificationArchiveRecordEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateImportErrorEntity;
import com.guoyu.platform.certification.entity.CertificationRiskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskChecklistItemEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamEquipmentEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamSiteEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoExamStaffEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamEquipmentMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamSiteMapper;
import com.guoyu.platform.certification.info.mapper.CertificationInfoExamStaffMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveImportBatchMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveImportErrorMapper;
import com.guoyu.platform.certification.mapper.CertificationArchiveRecordMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateImportErrorMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationRiskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskChecklistItemMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.vo.CertificationDashboardVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CertificationDashboardApplicationService {

  private static final String ARCHIVED = "ARCHIVED";
  private static final int ALERT_LIMIT = 10;

  private final CertificationTaskMapper taskMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationRiskMapper riskMapper;
  private final CertificationTaskChecklistItemMapper checklistMapper;
  private final CertificationCandidateImportErrorMapper candidateImportErrorMapper;
  private final CertificationArchiveRecordMapper archiveRecordMapper;
  private final CertificationArchiveImportBatchMapper archiveImportBatchMapper;
  private final CertificationArchiveImportErrorMapper archiveImportErrorMapper;
  private final CertificationInfoAssessorMapper assessorMapper;
  private final CertificationInfoExamStaffMapper examStaffMapper;
  private final CertificationInfoExamSiteMapper examSiteMapper;
  private final CertificationInfoExamEquipmentMapper equipmentMapper;

  public CertificationDashboardApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationRiskMapper riskMapper,
      CertificationTaskChecklistItemMapper checklistMapper,
      CertificationCandidateImportErrorMapper candidateImportErrorMapper,
      CertificationArchiveRecordMapper archiveRecordMapper,
      CertificationArchiveImportBatchMapper archiveImportBatchMapper,
      CertificationArchiveImportErrorMapper archiveImportErrorMapper,
      CertificationInfoAssessorMapper assessorMapper,
      CertificationInfoExamStaffMapper examStaffMapper,
      CertificationInfoExamSiteMapper examSiteMapper,
      CertificationInfoExamEquipmentMapper equipmentMapper
  ) {
    this.taskMapper = taskMapper;
    this.scopeMapper = scopeMapper;
    this.candidateMapper = candidateMapper;
    this.riskMapper = riskMapper;
    this.checklistMapper = checklistMapper;
    this.candidateImportErrorMapper = candidateImportErrorMapper;
    this.archiveRecordMapper = archiveRecordMapper;
    this.archiveImportBatchMapper = archiveImportBatchMapper;
    this.archiveImportErrorMapper = archiveImportErrorMapper;
    this.assessorMapper = assessorMapper;
    this.examStaffMapper = examStaffMapper;
    this.examSiteMapper = examSiteMapper;
    this.equipmentMapper = equipmentMapper;
  }

  @Transactional(readOnly = true)
  public CertificationDashboardVO getDashboard(CertificationDashboardQueryReq req) {
    LocalDate endDate = req.getEndDate() == null ? LocalDate.now() : req.getEndDate();
    LocalDate startDate = req.getStartDate() == null
        ? endDate.minusMonths(11).withDayOfMonth(1)
        : req.getStartDate();
    if (startDate.isAfter(endDate)) {
      LocalDate originalStart = startDate;
      startDate = endDate;
      endDate = originalStart;
    }
    String dateMode = normalizeDateMode(req.getDateMode());

    List<CertificationTaskEntity> allTasks = safeList(taskMapper.selectList(null));
    List<CertificationTaskScopeEntity> allScopes = safeList(scopeMapper.selectList(null));
    List<CertificationArchiveRecordEntity> allArchiveRecords = safeList(archiveRecordMapper.selectList(null));
    Map<Long, List<CertificationTaskScopeEntity>> scopesByTask = allScopes.stream()
        .filter(item -> item.getTaskId() != null)
        .collect(Collectors.groupingBy(CertificationTaskScopeEntity::getTaskId));
    Map<Long, CertificationArchiveRecordEntity> archiveByTask = allArchiveRecords.stream()
        .filter(item -> item.getTaskId() != null)
        .collect(Collectors.toMap(
            CertificationArchiveRecordEntity::getTaskId,
            Function.identity(),
            (left, right) -> left.getUpdatedAt() != null
                && right.getUpdatedAt() != null
                && left.getUpdatedAt().isAfter(right.getUpdatedAt()) ? left : right
        ));

    LocalDate effectiveStartDate = startDate;
    LocalDate effectiveEndDate = endDate;
    List<CertificationTaskEntity> tasks = allTasks.stream()
        .filter(task -> matchesTask(
            task,
            scopesByTask.getOrDefault(task.getId(), List.of()),
            archiveByTask.get(task.getId()),
            req,
            dateMode,
            effectiveStartDate,
            effectiveEndDate
        ))
        .toList();
    Set<Long> taskIds = tasks.stream()
        .map(CertificationTaskEntity::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    List<CertificationTaskScopeEntity> scopes = allScopes.stream()
        .filter(item -> taskIds.contains(item.getTaskId()))
        .toList();
    List<CertificationCandidateEntity> candidates = selectByTaskIds(candidateMapper, taskIds);
    List<CertificationRiskEntity> risks = selectRisks(taskIds);
    List<CertificationTaskChecklistItemEntity> checklistItems = selectChecklistItems(taskIds);
    List<CertificationCandidateImportErrorEntity> candidateImportErrors = selectCandidateImportErrors(taskIds);
    List<CertificationArchiveRecordEntity> archiveRecords = allArchiveRecords.stream()
        .filter(item -> taskIds.contains(item.getTaskId()))
        .toList();

    Map<Long, CertificationTaskEntity> taskById = tasks.stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(CertificationTaskEntity::getId, Function.identity()));
    Map<Long, List<CertificationCandidateEntity>> candidatesByTask = candidates.stream()
        .filter(item -> item.getTaskId() != null)
        .collect(Collectors.groupingBy(CertificationCandidateEntity::getTaskId));

    CertificationDashboardVO.Summary summary = buildSummary(
        tasks,
        scopes,
        candidates,
        archiveRecords,
        archiveByTask
    );
    CertificationDashboardVO.QualitySummary quality = buildQualitySummary(
        risks,
        checklistItems,
        candidates,
        candidateImportErrors,
        taskIds,
        archiveRecords
    );

    return new CertificationDashboardVO(
        summary,
        quality,
        buildFilterOptions(allScopes),
        buildPhaseDistribution(tasks, candidatesByTask),
        buildDimensionDistribution(scopes, candidates, true),
        buildDimensionDistribution(scopes, candidates, false),
        buildWorkTypeLevelDistribution(candidates),
        buildMonthlyTrends(tasks, archiveRecords, startDate, endDate),
        buildResources(),
        buildRiskAlerts(risks, taskById),
        buildComplianceAlerts(tasks, archiveByTask, candidateImportErrors)
    );
  }

  private CertificationDashboardVO.Summary buildSummary(
      List<CertificationTaskEntity> tasks,
      List<CertificationTaskScopeEntity> scopes,
      List<CertificationCandidateEntity> candidates,
      List<CertificationArchiveRecordEntity> archiveRecords,
      Map<Long, CertificationArchiveRecordEntity> archiveByTask
  ) {
    long totalTasks = tasks.size();
    long completedTasks = tasks.stream().filter(this::isCompleted).count();
    long inProgressTasks = totalTasks - completedTasks;
    long plannedPeopleCount = scopes.stream()
        .map(CertificationTaskScopeEntity::getPlannedPeopleCount)
        .filter(Objects::nonNull)
        .mapToLong(Integer::longValue)
        .sum();
    long actualPeopleCount = candidates.size();
    long overdueTasks = tasks.stream()
        .filter(task -> task.getPlanEndDate() != null)
        .filter(task -> task.getPlanEndDate().isBefore(LocalDate.now()))
        .filter(task -> !isCompleted(task))
        .count();
    long archivedTasks = archiveRecords.stream()
        .filter(item -> ARCHIVED.equalsIgnoreCase(item.getArchiveStatus()))
        .map(CertificationArchiveRecordEntity::getTaskId)
        .filter(Objects::nonNull)
        .distinct()
        .count();
    long archiveEligibleTasks = tasks.stream()
        .filter(task -> CertificationTaskPhases.ARCHIVE.equals(task.getCurrentPhase())
            || CertificationTaskPhases.CERTIFICATION_COMPLETED.equals(task.getCurrentPhase()))
        .count();
    long pendingArchiveTasks = tasks.stream()
        .filter(task -> CertificationTaskPhases.ARCHIVE.equals(task.getCurrentPhase()))
        .filter(task -> {
          CertificationArchiveRecordEntity record = archiveByTask.get(task.getId());
          return record == null || !ARCHIVED.equalsIgnoreCase(record.getArchiveStatus());
        })
        .count();
    long materialCompletedTasks = archiveRecords.stream()
        .filter(item -> Boolean.TRUE.equals(item.getMaterialArchived()))
        .map(CertificationArchiveRecordEntity::getTaskId)
        .filter(Objects::nonNull)
        .distinct()
        .count();
    double averageCycleDays = tasks.stream()
        .filter(this::isCompleted)
        .map(task -> cycleDays(task, archiveByTask.get(task.getId())))
        .filter(Objects::nonNull)
        .mapToLong(Long::longValue)
        .average()
        .orElse(0);

    return new CertificationDashboardVO.Summary(
        totalTasks,
        inProgressTasks,
        completedTasks,
        percentage(completedTasks, totalTasks),
        plannedPeopleCount,
        actualPeopleCount,
        overdueTasks,
        roundOne(averageCycleDays),
        archivedTasks,
        pendingArchiveTasks,
        percentage(archivedTasks, archiveEligibleTasks),
        percentage(materialCompletedTasks, archiveEligibleTasks)
    );
  }

  private CertificationDashboardVO.QualitySummary buildQualitySummary(
      List<CertificationRiskEntity> risks,
      List<CertificationTaskChecklistItemEntity> checklistItems,
      List<CertificationCandidateEntity> candidates,
      List<CertificationCandidateImportErrorEntity> candidateImportErrors,
      Set<Long> taskIds,
      List<CertificationArchiveRecordEntity> archiveRecords
  ) {
    List<CertificationRiskEntity> openRisks = risks.stream().filter(this::isOpenRisk).toList();
    long highRiskTaskCount = openRisks.stream()
        .filter(this::isHighRisk)
        .map(CertificationRiskEntity::getTaskId)
        .filter(Objects::nonNull)
        .distinct()
        .count();
    long checkedItems = checklistItems.stream()
        .filter(item -> Integer.valueOf(1).equals(item.getChecked()))
        .count();
    long completeCandidates = candidates.stream()
        .filter(item -> Boolean.TRUE.equals(item.getDocumentComplete()))
        .count();
    long archiveImportErrors = countArchiveImportErrors(taskIds, archiveRecords);

    return new CertificationDashboardVO.QualitySummary(
        openRisks.size(),
        highRiskTaskCount,
        percentage(checkedItems, checklistItems.size()),
        percentage(completeCandidates, candidates.size()),
        candidateImportErrors.size() + archiveImportErrors
    );
  }

  private CertificationDashboardVO.FilterOptions buildFilterOptions(
      List<CertificationTaskScopeEntity> scopes
  ) {
    return new CertificationDashboardVO.FilterOptions(
        distinctSorted(scopes.stream().map(CertificationTaskScopeEntity::getCertificationType).toList()),
        distinctSorted(scopes.stream().map(CertificationTaskScopeEntity::getWorkTypeName).toList()),
        distinctSorted(scopes.stream().map(CertificationTaskScopeEntity::getLevelName).toList())
    );
  }

  private List<CertificationDashboardVO.DistributionItem> buildPhaseDistribution(
      List<CertificationTaskEntity> tasks,
      Map<Long, List<CertificationCandidateEntity>> candidatesByTask
  ) {
    Map<String, List<CertificationTaskEntity>> grouped = tasks.stream()
        .collect(Collectors.groupingBy(
            task -> text(task.getCurrentPhase(), "unknown"),
            LinkedHashMap::new,
            Collectors.toList()
        ));
    return CertificationTaskPhases.ORDER.stream()
        .map(phase -> {
          List<CertificationTaskEntity> phaseTasks = grouped.getOrDefault(phase, List.of());
          long peopleCount = phaseTasks.stream()
              .map(CertificationTaskEntity::getId)
              .mapToLong(id -> candidatesByTask.getOrDefault(id, List.of()).size())
              .sum();
          return new CertificationDashboardVO.DistributionItem(
              phase,
              CertificationTaskPhases.phaseName(phase),
              phaseTasks.size(),
              peopleCount
          );
        })
        .toList();
  }

  private List<CertificationDashboardVO.DistributionItem> buildDimensionDistribution(
      List<CertificationTaskScopeEntity> scopes,
      List<CertificationCandidateEntity> candidates,
      boolean byWorkType
  ) {
    Map<String, Set<Long>> tasksByDimension = new HashMap<>();
    for (CertificationTaskScopeEntity scope : scopes) {
      String value = byWorkType ? scope.getWorkTypeName() : scope.getLevelName();
      if (!StringUtils.hasText(value) || scope.getTaskId() == null) {
        continue;
      }
      tasksByDimension.computeIfAbsent(value.trim(), key -> new HashSet<>()).add(scope.getTaskId());
    }
    Map<String, Long> peopleByDimension = candidates.stream()
        .map(candidate -> byWorkType ? candidate.getWorkTypeName() : candidate.getLevelName())
        .filter(StringUtils::hasText)
        .map(String::trim)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    Set<String> dimensions = new HashSet<>(tasksByDimension.keySet());
    dimensions.addAll(peopleByDimension.keySet());
    return dimensions.stream()
        .map(value -> new CertificationDashboardVO.DistributionItem(
            value,
            value,
            tasksByDimension.getOrDefault(value, Set.of()).size(),
            peopleByDimension.getOrDefault(value, 0L)
        ))
        .sorted(Comparator.comparingLong(CertificationDashboardVO.DistributionItem::peopleCount).reversed()
            .thenComparing(CertificationDashboardVO.DistributionItem::name))
        .toList();
  }

  private List<CertificationDashboardVO.WorkTypeLevelItem> buildWorkTypeLevelDistribution(
      List<CertificationCandidateEntity> candidates
  ) {
    Map<String, Long> grouped = candidates.stream()
        .filter(item -> StringUtils.hasText(item.getWorkTypeName()))
        .filter(item -> StringUtils.hasText(item.getLevelName()))
        .collect(Collectors.groupingBy(
            item -> item.getWorkTypeName().trim() + "\u0000" + item.getLevelName().trim(),
            Collectors.counting()
        ));
    return grouped.entrySet().stream()
        .map(entry -> {
          String[] parts = entry.getKey().split("\u0000", 2);
          return new CertificationDashboardVO.WorkTypeLevelItem(parts[0], parts[1], entry.getValue());
        })
        .sorted(Comparator.comparingLong(CertificationDashboardVO.WorkTypeLevelItem::peopleCount).reversed())
        .toList();
  }

  private List<CertificationDashboardVO.MonthlyTrendItem> buildMonthlyTrends(
      List<CertificationTaskEntity> tasks,
      List<CertificationArchiveRecordEntity> archiveRecords,
      LocalDate startDate,
      LocalDate endDate
  ) {
    YearMonth endMonth = YearMonth.from(endDate);
    YearMonth startMonth = YearMonth.from(startDate);
    if (ChronoUnit.MONTHS.between(startMonth, endMonth) > 23) {
      startMonth = endMonth.minusMonths(23);
    }
    List<CertificationDashboardVO.MonthlyTrendItem> result = new ArrayList<>();
    for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
      YearMonth currentMonth = month;
      long created = tasks.stream()
          .filter(task -> task.getCreatedAt() != null)
          .filter(task -> YearMonth.from(task.getCreatedAt()).equals(currentMonth))
          .count();
      List<CertificationArchiveRecordEntity> archivedThisMonth = archiveRecords.stream()
          .filter(record -> ARCHIVED.equalsIgnoreCase(record.getArchiveStatus()))
          .filter(record -> record.getArchivedAt() != null)
          .filter(record -> YearMonth.from(record.getArchivedAt()).equals(currentMonth))
          .toList();
      long archivedPeople = archivedThisMonth.stream()
          .map(CertificationArchiveRecordEntity::getCandidateCount)
          .filter(Objects::nonNull)
          .mapToLong(Integer::longValue)
          .sum();
      result.add(new CertificationDashboardVO.MonthlyTrendItem(
          currentMonth.toString(),
          created,
          archivedThisMonth.size(),
          archivedPeople
      ));
    }
    return result;
  }

  private List<CertificationDashboardVO.ResourceItem> buildResources() {
    LocalDate expiryDate = LocalDate.now().plusDays(30);
    List<CertificationInfoAssessorEntity> assessors = safeList(assessorMapper.selectList(null));
    List<CertificationInfoExamStaffEntity> examStaff = safeList(examStaffMapper.selectList(null));
    List<CertificationInfoExamSiteEntity> sites = safeList(examSiteMapper.selectList(null));
    List<CertificationInfoExamEquipmentEntity> equipment = safeList(equipmentMapper.selectList(null));

    return List.of(
        new CertificationDashboardVO.ResourceItem(
            "assessor",
            "考评人员",
            assessors.size(),
            assessors.stream().filter(item -> isApproved(item.getAuditStatus()) && isIdle(item.getWorkStatus())).count(),
            assessors.stream().filter(item -> isApproved(item.getAuditStatus()) && !isIdle(item.getWorkStatus())).count(),
            assessors.stream().filter(item -> isDisabled(item.getAuditStatus())).count(),
            assessors.stream().filter(item -> isExpiring(item.getValidUntil(), expiryDate)).count(),
            0
        ),
        new CertificationDashboardVO.ResourceItem(
            "exam_staff",
            "考务人员",
            examStaff.size(),
            examStaff.stream().filter(item -> isApproved(item.getAuditStatus()) && isIdle(item.getWorkStatus())).count(),
            examStaff.stream().filter(item -> isApproved(item.getAuditStatus()) && !isIdle(item.getWorkStatus())).count(),
            examStaff.stream().filter(item -> isDisabled(item.getAuditStatus())).count(),
            0,
            0
        ),
        new CertificationDashboardVO.ResourceItem(
            "exam_site",
            "认定考点",
            sites.size(),
            sites.stream().filter(this::isAvailableSite).count(),
            sites.stream().filter(item -> isApproved(item.getAuditStatus()) && !isAvailableSite(item)).count(),
            sites.stream().filter(item -> isDisabled(item.getAuditStatus())).count(),
            0,
            sites.stream().filter(this::isAvailableSite)
                .map(CertificationInfoExamSiteEntity::getSeatCount)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum()
        ),
        new CertificationDashboardVO.ResourceItem(
            "equipment",
            "认定设备",
            equipment.stream().map(CertificationInfoExamEquipmentEntity::getEquipmentQuantity)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum(),
            equipment.stream().filter(this::isAvailableEquipment)
                .map(CertificationInfoExamEquipmentEntity::getEquipmentQuantity)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum(),
            equipment.stream().filter(item -> isApproved(item.getAuditStatus()) && !isAvailableEquipment(item))
                .map(CertificationInfoExamEquipmentEntity::getEquipmentQuantity)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum(),
            equipment.stream().filter(item -> isDisabled(item.getAuditStatus()))
                .map(CertificationInfoExamEquipmentEntity::getEquipmentQuantity)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum(),
            equipment.stream().filter(item -> isExpiring(item.getCalibrationValidUntil(), expiryDate))
                .map(CertificationInfoExamEquipmentEntity::getEquipmentQuantity)
                .filter(Objects::nonNull).mapToLong(Integer::longValue).sum(),
            0
        )
    );
  }

  private List<CertificationDashboardVO.AlertItem> buildRiskAlerts(
      List<CertificationRiskEntity> risks,
      Map<Long, CertificationTaskEntity> taskById
  ) {
    return risks.stream()
        .filter(this::isOpenRisk)
        .filter(this::isHighRisk)
        .map(risk -> {
          CertificationTaskEntity task = taskById.get(risk.getTaskId());
          return new CertificationDashboardVO.AlertItem(
              risk.getTaskId(),
              task == null ? null : task.getTaskNo(),
              task == null ? null : task.getTaskName(),
              "HIGH_RISK",
              text(risk.getRiskLevelCode(), "high"),
              risk.getRiskDescription(),
              risk.getExpectedCloseDate()
          );
        })
        .sorted(Comparator.comparing(
            CertificationDashboardVO.AlertItem::dueDate,
            Comparator.nullsLast(Comparator.naturalOrder())
        ))
        .limit(ALERT_LIMIT)
        .toList();
  }

  private List<CertificationDashboardVO.AlertItem> buildComplianceAlerts(
      List<CertificationTaskEntity> tasks,
      Map<Long, CertificationArchiveRecordEntity> archiveByTask,
      List<CertificationCandidateImportErrorEntity> candidateImportErrors
  ) {
    Map<Long, Long> importErrorsByTask = candidateImportErrors.stream()
        .filter(item -> item.getTaskId() != null)
        .collect(Collectors.groupingBy(CertificationCandidateImportErrorEntity::getTaskId, Collectors.counting()));
    List<CertificationDashboardVO.AlertItem> alerts = new ArrayList<>();
    for (CertificationTaskEntity task : tasks) {
      CertificationArchiveRecordEntity archive = archiveByTask.get(task.getId());
      if (CertificationTaskPhases.ARCHIVE.equals(task.getCurrentPhase())
          && (archive == null || !ARCHIVED.equalsIgnoreCase(archive.getArchiveStatus()))) {
        alerts.add(alert(task, "PENDING_ARCHIVE", "warning", "任务已进入认定归档阶段，尚未完成归档"));
      }
      if (archive != null && !Boolean.TRUE.equals(archive.getMaterialArchived())) {
        alerts.add(alert(task, "MATERIAL_INCOMPLETE", "warning", "任务归档材料尚未完整"));
      }
      long errorCount = importErrorsByTask.getOrDefault(task.getId(), 0L);
      if (errorCount > 0) {
        alerts.add(alert(task, "IMPORT_ERROR", "error", "存在 " + errorCount + " 条考生导入异常"));
      }
      if (task.getPlanEndDate() != null
          && task.getPlanEndDate().isBefore(LocalDate.now())
          && !isCompleted(task)) {
        alerts.add(new CertificationDashboardVO.AlertItem(
            task.getId(),
            task.getTaskNo(),
            task.getTaskName(),
            "OVERDUE",
            "error",
            "任务已超过计划结束日期",
            task.getPlanEndDate()
        ));
      }
    }
    return alerts.stream()
        .sorted(Comparator.comparing(
            CertificationDashboardVO.AlertItem::dueDate,
            Comparator.nullsLast(Comparator.naturalOrder())
        ))
        .limit(ALERT_LIMIT)
        .toList();
  }

  private boolean matchesTask(
      CertificationTaskEntity task,
      List<CertificationTaskScopeEntity> scopes,
      CertificationArchiveRecordEntity archive,
      CertificationDashboardQueryReq req,
      String dateMode,
      LocalDate startDate,
      LocalDate endDate
  ) {
    if (StringUtils.hasText(req.getCurrentPhase())
        && !req.getCurrentPhase().equals(task.getCurrentPhase())) {
      return false;
    }
    if (StringUtils.hasText(req.getStatus()) && !req.getStatus().equals(task.getStatus())) {
      return false;
    }
    if (StringUtils.hasText(req.getCertificationType())
        && scopes.stream().noneMatch(item -> req.getCertificationType().equals(item.getCertificationType()))) {
      return false;
    }
    if (StringUtils.hasText(req.getWorkTypeName())
        && scopes.stream().noneMatch(item -> req.getWorkTypeName().equals(item.getWorkTypeName()))) {
      return false;
    }
    if (StringUtils.hasText(req.getLevelName())
        && scopes.stream().noneMatch(item -> req.getLevelName().equals(item.getLevelName()))) {
      return false;
    }
    LocalDate businessDate = switch (dateMode) {
      case "exam" -> task.getExamDate();
      case "archive" -> archive == null || archive.getArchivedAt() == null
          ? null : archive.getArchivedAt().toLocalDate();
      default -> task.getCreatedAt() == null ? null : task.getCreatedAt().toLocalDate();
    };
    return businessDate != null && !businessDate.isBefore(startDate) && !businessDate.isAfter(endDate);
  }

  private long countArchiveImportErrors(
      Set<Long> taskIds,
      List<CertificationArchiveRecordEntity> archiveRecords
  ) {
    if (taskIds.isEmpty() || archiveRecords.isEmpty()) {
      return 0;
    }
    Set<Long> archiveIds = archiveRecords.stream()
        .map(CertificationArchiveRecordEntity::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (archiveIds.isEmpty()) {
      return 0;
    }
    List<CertificationArchiveImportBatchEntity> batches = safeList(archiveImportBatchMapper.selectList(
        new LambdaQueryWrapper<CertificationArchiveImportBatchEntity>()
            .in(CertificationArchiveImportBatchEntity::getArchiveId, archiveIds)
    ));
    Set<Long> batchIds = batches.stream()
        .map(CertificationArchiveImportBatchEntity::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (batchIds.isEmpty()) {
      return 0;
    }
    List<CertificationArchiveImportErrorEntity> errors = safeList(archiveImportErrorMapper.selectList(
        new LambdaQueryWrapper<CertificationArchiveImportErrorEntity>()
            .in(CertificationArchiveImportErrorEntity::getBatchId, batchIds)
    ));
    return errors.size();
  }

  private List<CertificationCandidateEntity> selectByTaskIds(
      CertificationCandidateMapper mapper,
      Set<Long> taskIds
  ) {
    if (taskIds.isEmpty()) {
      return List.of();
    }
    return safeList(mapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .in(CertificationCandidateEntity::getTaskId, taskIds)
    ));
  }

  private List<CertificationRiskEntity> selectRisks(Set<Long> taskIds) {
    if (taskIds.isEmpty()) {
      return List.of();
    }
    return safeList(riskMapper.selectList(
        new LambdaQueryWrapper<CertificationRiskEntity>()
            .in(CertificationRiskEntity::getTaskId, taskIds)
    ));
  }

  private List<CertificationTaskChecklistItemEntity> selectChecklistItems(Set<Long> taskIds) {
    if (taskIds.isEmpty()) {
      return List.of();
    }
    return safeList(checklistMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskChecklistItemEntity>()
            .in(CertificationTaskChecklistItemEntity::getTaskId, taskIds)
    ));
  }

  private List<CertificationCandidateImportErrorEntity> selectCandidateImportErrors(Set<Long> taskIds) {
    if (taskIds.isEmpty()) {
      return List.of();
    }
    return safeList(candidateImportErrorMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateImportErrorEntity>()
            .in(CertificationCandidateImportErrorEntity::getTaskId, taskIds)
    ));
  }

  private CertificationDashboardVO.AlertItem alert(
      CertificationTaskEntity task,
      String type,
      String level,
      String description
  ) {
    return new CertificationDashboardVO.AlertItem(
        task.getId(),
        task.getTaskNo(),
        task.getTaskName(),
        type,
        level,
        description,
        task.getPlanEndDate()
    );
  }

  private Long cycleDays(
      CertificationTaskEntity task,
      CertificationArchiveRecordEntity archive
  ) {
    if (task.getCreatedAt() == null) {
      return null;
    }
    LocalDateTime completedAt = archive != null && archive.getArchivedAt() != null
        ? archive.getArchivedAt()
        : task.getUpdatedAt();
    if (completedAt == null || completedAt.isBefore(task.getCreatedAt())) {
      return null;
    }
    return ChronoUnit.DAYS.between(task.getCreatedAt().toLocalDate(), completedAt.toLocalDate());
  }

  private boolean isCompleted(CertificationTaskEntity task) {
    return CertificationTaskPhases.CERTIFICATION_COMPLETED.equals(task.getCurrentPhase())
        || CertificationTaskStatuses.COMPLETED.equals(task.getStatus());
  }

  private boolean isOpenRisk(CertificationRiskEntity risk) {
    String status = text(risk.getStatus(), "").toLowerCase(Locale.ROOT);
    return !Set.of("closed", "resolved", "completed").contains(status);
  }

  private boolean isHighRisk(CertificationRiskEntity risk) {
    return risk.getRiskLevelScore() != null && risk.getRiskLevelScore() >= 4
        || text(risk.getRiskLevelCode(), "").toLowerCase(Locale.ROOT).contains("high");
  }

  private boolean isAvailableSite(CertificationInfoExamSiteEntity item) {
    String status = text(item.getStatus(), "").toLowerCase(Locale.ROOT);
    return isApproved(item.getAuditStatus())
        && !Set.of("occupied", "busy", "disabled", "maintenance").contains(status);
  }

  private boolean isAvailableEquipment(CertificationInfoExamEquipmentEntity item) {
    String status = text(item.getEquipmentStatus(), "").toLowerCase(Locale.ROOT);
    return isApproved(item.getAuditStatus())
        && !Set.of("occupied", "busy", "disabled", "maintenance", "fault").contains(status);
  }

  private boolean isApproved(String status) {
    return Set.of("approved", "enabled").contains(text(status, "").toLowerCase(Locale.ROOT));
  }

  private boolean isDisabled(String status) {
    return Set.of("disabled", "rejected").contains(text(status, "").toLowerCase(Locale.ROOT));
  }

  private boolean isIdle(String status) {
    return Set.of("idle", "available", "normal").contains(text(status, "").toLowerCase(Locale.ROOT));
  }

  private boolean isExpiring(LocalDate value, LocalDate expiryDate) {
    return value != null && !value.isBefore(LocalDate.now()) && !value.isAfter(expiryDate);
  }

  private String normalizeDateMode(String value) {
    return value != null && Set.of("created", "exam", "archive").contains(value) ? value : "created";
  }

  private List<String> distinctSorted(List<String> values) {
    return values.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .sorted()
        .toList();
  }

  private double percentage(long numerator, long denominator) {
    return denominator <= 0 ? 0 : Math.round(numerator * 1000.0 / denominator) / 10.0;
  }

  private double roundOne(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private String text(String value, String fallback) {
    return StringUtils.hasText(value) ? value : fallback;
  }

  private <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }
}
