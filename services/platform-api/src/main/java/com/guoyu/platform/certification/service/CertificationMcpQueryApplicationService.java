package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.dto.CertificationMcpTaskQueryReq;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalCandidateResultEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalQuestionResultEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionExceptionEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionStatusEntity;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalCandidateResultMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalQuestionResultMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.mapper.PracticalCollectionExceptionMapper;
import com.guoyu.platform.certification.mapper.PracticalCollectionStatusMapper;
import com.guoyu.platform.certification.vo.CertificationMcpCollectionProgressVO;
import com.guoyu.platform.certification.vo.CertificationMcpPracticalScoreProgressVO;
import com.guoyu.platform.certification.vo.CertificationMcpTaskItemVO;
import com.guoyu.platform.certification.vo.CertificationMcpTaskOverviewVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CertificationMcpQueryApplicationService {

  private static final int DETAIL_LIMIT = 200;

  private final CertificationTaskMapper taskMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationTaskAssessorMapper taskAssessorMapper;
  private final PracticalCollectionStatusMapper collectionStatusMapper;
  private final PracticalCollectionExceptionMapper collectionExceptionMapper;
  private final CertificationPracticalCandidateResultMapper candidateResultMapper;
  private final CertificationPracticalQuestionResultMapper questionResultMapper;
  private final CertificationAssessorTaskMapper assessorTaskMapper;

  public CertificationMcpQueryApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationTaskAssessorMapper taskAssessorMapper,
      PracticalCollectionStatusMapper collectionStatusMapper,
      PracticalCollectionExceptionMapper collectionExceptionMapper,
      CertificationPracticalCandidateResultMapper candidateResultMapper,
      CertificationPracticalQuestionResultMapper questionResultMapper,
      CertificationAssessorTaskMapper assessorTaskMapper
  ) {
    this.taskMapper = taskMapper;
    this.scopeMapper = scopeMapper;
    this.candidateMapper = candidateMapper;
    this.taskAssessorMapper = taskAssessorMapper;
    this.collectionStatusMapper = collectionStatusMapper;
    this.collectionExceptionMapper = collectionExceptionMapper;
    this.candidateResultMapper = candidateResultMapper;
    this.questionResultMapper = questionResultMapper;
    this.assessorTaskMapper = assessorTaskMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CertificationMcpTaskItemVO> searchTasks(CertificationMcpTaskQueryReq req) {
    Long tenantId = currentTenantId();
    LambdaQueryWrapper<CertificationTaskEntity> wrapper = new LambdaQueryWrapper<CertificationTaskEntity>()
        .eq(CertificationTaskEntity::getTenantId, tenantId)
        .eq(CertificationTaskEntity::getDeleted, 0)
        .and(StringUtils.hasText(req.getKeyword()), nested -> nested
            .like(CertificationTaskEntity::getTaskName, req.getKeyword())
            .or()
            .like(CertificationTaskEntity::getTaskNo, req.getKeyword())
            .or()
            .like(CertificationTaskEntity::getTaskManagerName, req.getKeyword())
            .or()
            .like(CertificationTaskEntity::getCompanyName, req.getKeyword()))
        .eq(StringUtils.hasText(req.getStatus()), CertificationTaskEntity::getStatus, req.getStatus());
    applyDateRange(wrapper, req.getStartDate(), req.getEndDate());
    wrapper.orderByDesc(CertificationTaskEntity::getLastBusinessUpdatedAt)
        .orderByDesc(CertificationTaskEntity::getCreatedAt);

    Page<CertificationTaskEntity> page = taskMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<CertificationTaskEntity> tasks = safeList(page.getRecords());
    List<Long> taskIds = tasks.stream().map(CertificationTaskEntity::getId).filter(Objects::nonNull).toList();
    Map<Long, List<CertificationTaskScopeEntity>> scopeMap = scopesByTask(tenantId, taskIds);
    Map<Long, Long> candidateCounts = candidateCountsByTask(tenantId, taskIds);
    List<CertificationMcpTaskItemVO> items = tasks.stream()
        .map(task -> toTaskItem(task, scopeMap.getOrDefault(task.getId(), List.of()),
            candidateCounts.getOrDefault(task.getId(), 0L)))
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CertificationMcpTaskOverviewVO getTaskOverview(Long taskId) {
    Long tenantId = currentTenantId();
    CertificationTaskEntity task = findTask(tenantId, taskId);
    if (task == null) {
      return emptyOverview(taskId);
    }
    List<CertificationTaskScopeEntity> scopes = scopesByTask(tenantId, List.of(taskId)).getOrDefault(taskId, List.of());
    long candidateCount = candidateCount(tenantId, taskId);
    long assessorCount = safeList(taskAssessorMapper.selectList(new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTenantId, tenantId)
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .eq(CertificationTaskAssessorEntity::getDeleted, 0)))
        .stream()
        .map(CertificationTaskAssessorEntity::getAssessorId)
        .filter(Objects::nonNull)
        .distinct()
        .count();
    long collectionCompleted = collectionCompletedCount(tenantId, taskId);
    long practicalCompleted = practicalCompletedCount(tenantId, taskId);
    return new CertificationMcpTaskOverviewVO(
        id(task.getId()),
        task.getTaskNo(),
        task.getTaskName(),
        task.getStatus(),
        normalizePhase(task.getCurrentPhase()),
        CertificationTaskPhases.phaseName(normalizePhase(task.getCurrentPhase())),
        summarize(scopes, CertificationTaskScopeEntity::getWorkTypeName),
        summarize(scopes, CertificationTaskScopeEntity::getLevelName),
        task.getExamDate(),
        candidateCount,
        assessorCount,
        collectionCompleted,
        candidateCount,
        percentage(collectionCompleted, candidateCount),
        practicalCompleted,
        candidateCount,
        percentage(practicalCompleted, candidateCount)
    );
  }

  @Transactional(readOnly = true)
  public CertificationMcpCollectionProgressVO getCollectionProgress(Long taskId) {
    Long tenantId = currentTenantId();
    if (findTask(tenantId, taskId) == null) {
      return new CertificationMcpCollectionProgressVO(id(taskId), 0, 0, 0, 0, List.of(), List.of());
    }
    List<CertificationCandidateEntity> candidates = candidates(tenantId, taskId);
    Map<Long, PracticalCollectionStatusEntity> statusMap = collectionStatuses(tenantId, taskId).stream()
        .filter(item -> item.getCandidateId() != null)
        .collect(Collectors.toMap(
            PracticalCollectionStatusEntity::getCandidateId,
            Function.identity(),
            (left, right) -> left));
    List<CertificationMcpCollectionProgressVO.IncompleteCandidate> incompleteCandidates = candidates.stream()
        .map(candidate -> incompleteCandidate(candidate, statusMap.get(candidate.getId())))
        .filter(Objects::nonNull)
        .limit(DETAIL_LIMIT)
        .toList();
    long completed = candidates.stream()
        .map(candidate -> statusMap.get(candidate.getId()))
        .filter(this::isCollectionCompleted)
        .count();
    Map<Long, CertificationCandidateEntity> candidateMap = candidates.stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(CertificationCandidateEntity::getId, Function.identity(), (left, right) -> left));
    List<CertificationMcpCollectionProgressVO.CollectionExceptionItem> exceptionItems =
        collectionExceptions(tenantId, taskId).stream()
            .map(item -> collectionExceptionItem(item, candidateMap.get(item.getCandidateId())))
            .toList();
    long total = candidates.size();
    return new CertificationMcpCollectionProgressVO(
        id(taskId),
        total,
        completed,
        Math.max(total - completed, 0),
        percentage(completed, total),
        incompleteCandidates,
        exceptionItems
    );
  }

  @Transactional(readOnly = true)
  public CertificationMcpPracticalScoreProgressVO getPracticalScoreProgress(Long taskId) {
    Long tenantId = currentTenantId();
    if (findTask(tenantId, taskId) == null) {
      return new CertificationMcpPracticalScoreProgressVO(id(taskId), 0, 0, 0, 0, List.of(), List.of());
    }
    List<CertificationCandidateEntity> candidates = candidates(tenantId, taskId);
    Map<Long, CertificationPracticalCandidateResultEntity> resultMap = practicalCandidateResults(tenantId, taskId)
        .stream()
        .filter(item -> item.getCandidateId() != null)
        .collect(Collectors.toMap(
            CertificationPracticalCandidateResultEntity::getCandidateId,
            Function.identity(),
            (left, right) -> left));
    long scored = candidates.stream()
        .map(candidate -> resultMap.get(candidate.getId()))
        .filter(this::isPracticalCompleted)
        .count();
    List<CertificationMcpPracticalScoreProgressVO.UnscoredCandidate> unscoredCandidates = candidates.stream()
        .filter(candidate -> !isPracticalCompleted(resultMap.get(candidate.getId())))
        .map(candidate -> unscoredCandidate(candidate, resultMap.get(candidate.getId())))
        .limit(DETAIL_LIMIT)
        .toList();
    List<CertificationMcpPracticalScoreProgressVO.AnomalyItem> anomalyItems =
        practicalAnomalies(tenantId, taskId, candidates, resultMap);
    long total = candidates.size();
    return new CertificationMcpPracticalScoreProgressVO(
        id(taskId),
        total,
        scored,
        Math.max(total - scored, 0),
        percentage(scored, total),
        unscoredCandidates,
        anomalyItems
    );
  }

  private void applyDateRange(
      LambdaQueryWrapper<CertificationTaskEntity> wrapper,
      LocalDate startDate,
      LocalDate endDate
  ) {
    if (startDate == null && endDate == null) {
      return;
    }
    LocalDate effectiveStart = startDate == null ? LocalDate.of(1970, 1, 1) : startDate;
    LocalDate effectiveEnd = endDate == null ? LocalDate.of(2999, 12, 31) : endDate;
    if (effectiveStart.isAfter(effectiveEnd)) {
      LocalDate temp = effectiveStart;
      effectiveStart = effectiveEnd;
      effectiveEnd = temp;
    }
    LocalDate finalStart = effectiveStart;
    LocalDate finalEnd = effectiveEnd;
    wrapper.and(range -> range
        .between(CertificationTaskEntity::getExamDate, finalStart, finalEnd)
        .or(plan -> plan
            .le(CertificationTaskEntity::getPlanStartDate, finalEnd)
            .ge(CertificationTaskEntity::getPlanEndDate, finalStart))
        .or(created -> created
            .ge(CertificationTaskEntity::getCreatedAt, finalStart.atStartOfDay())
            .lt(CertificationTaskEntity::getCreatedAt, finalEnd.plusDays(1).atStartOfDay())));
  }

  private CertificationMcpTaskItemVO toTaskItem(
      CertificationTaskEntity task,
      List<CertificationTaskScopeEntity> scopes,
      long candidateCount
  ) {
    String phase = normalizePhase(task.getCurrentPhase());
    return new CertificationMcpTaskItemVO(
        id(task.getId()),
        task.getTaskNo(),
        task.getTaskName(),
        task.getStatus(),
        phase,
        CertificationTaskPhases.phaseName(phase),
        summarize(scopes, CertificationTaskScopeEntity::getWorkTypeName),
        summarize(scopes, CertificationTaskScopeEntity::getLevelName),
        task.getPlannedPeopleCount(),
        candidateCount,
        task.getPlanStartDate(),
        task.getPlanEndDate(),
        task.getExamDate(),
        task.getLastBusinessUpdatedAt()
    );
  }

  private Map<Long, List<CertificationTaskScopeEntity>> scopesByTask(Long tenantId, List<Long> taskIds) {
    if (taskIds == null || taskIds.isEmpty()) {
      return Map.of();
    }
    return safeList(scopeMapper.selectList(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTenantId, tenantId)
            .in(CertificationTaskScopeEntity::getTaskId, taskIds)
            .eq(CertificationTaskScopeEntity::getDeleted, 0)
            .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
            .orderByAsc(CertificationTaskScopeEntity::getId)))
        .stream()
        .filter(item -> item.getTaskId() != null)
        .collect(Collectors.groupingBy(CertificationTaskScopeEntity::getTaskId));
  }

  private Map<Long, Long> candidateCountsByTask(Long tenantId, List<Long> taskIds) {
    if (taskIds == null || taskIds.isEmpty()) {
      return Map.of();
    }
    return safeList(candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .in(CertificationCandidateEntity::getTaskId, taskIds)
            .eq(CertificationCandidateEntity::getDeleted, 0)))
        .stream()
        .filter(item -> item.getTaskId() != null)
        .collect(Collectors.groupingBy(CertificationCandidateEntity::getTaskId, Collectors.counting()));
  }

  private long candidateCount(Long tenantId, Long taskId) {
    Long count = candidateMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTenantId, tenantId)
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .eq(CertificationCandidateEntity::getDeleted, 0));
    return count == null ? 0 : count;
  }

  private long collectionCompletedCount(Long tenantId, Long taskId) {
    Long count = collectionStatusMapper.selectCount(new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
        .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
        .eq(PracticalCollectionStatusEntity::getTaskId, taskId)
        .eq(PracticalCollectionStatusEntity::getOverallStatus, "COMPLETED")
        .eq(PracticalCollectionStatusEntity::getDeleted, 0));
    return count == null ? 0 : count;
  }

  private long practicalCompletedCount(Long tenantId, Long taskId) {
    Long count = candidateResultMapper.selectCount(new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
        .eq(CertificationPracticalCandidateResultEntity::getTenantId, tenantId)
        .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId)
        .eq(CertificationPracticalCandidateResultEntity::getStatus, "completed")
        .eq(CertificationPracticalCandidateResultEntity::getDeleted, 0));
    return count == null ? 0 : count;
  }

  private List<CertificationCandidateEntity> candidates(Long tenantId, Long taskId) {
    return safeList(candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTenantId, tenantId)
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .eq(CertificationCandidateEntity::getDeleted, 0)
        .orderByAsc(CertificationCandidateEntity::getId)));
  }

  private List<PracticalCollectionStatusEntity> collectionStatuses(Long tenantId, Long taskId) {
    return safeList(collectionStatusMapper.selectList(new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
        .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
        .eq(PracticalCollectionStatusEntity::getTaskId, taskId)
        .eq(PracticalCollectionStatusEntity::getDeleted, 0)));
  }

  private List<PracticalCollectionExceptionEntity> collectionExceptions(Long tenantId, Long taskId) {
    return safeList(collectionExceptionMapper.selectList(new LambdaQueryWrapper<PracticalCollectionExceptionEntity>()
        .eq(PracticalCollectionExceptionEntity::getTenantId, tenantId)
        .eq(PracticalCollectionExceptionEntity::getTaskId, taskId)
        .eq(PracticalCollectionExceptionEntity::getDeleted, 0)
        .orderByDesc(PracticalCollectionExceptionEntity::getHandledAt)
        .orderByDesc(PracticalCollectionExceptionEntity::getCreatedAt)
        .last("LIMIT " + DETAIL_LIMIT)));
  }

  private List<CertificationPracticalCandidateResultEntity> practicalCandidateResults(Long tenantId, Long taskId) {
    return safeList(candidateResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTenantId, tenantId)
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId)
            .eq(CertificationPracticalCandidateResultEntity::getDeleted, 0)));
  }

  private CertificationMcpCollectionProgressVO.IncompleteCandidate incompleteCandidate(
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status
  ) {
    if (isCollectionCompleted(status)) {
      return null;
    }
    return new CertificationMcpCollectionProgressVO.IncompleteCandidate(
        id(candidate.getId()),
        candidate.getCandidateNo(),
        candidate.getCandidateName(),
        candidate.getWorkTypeName(),
        candidate.getLevelName(),
        status == null ? null : status.getOverallStatus(),
        status == null ? null : status.getFingerprintStatus(),
        status == null ? null : status.getSignatureStatus(),
        collectionMissingItems(status)
    );
  }

  private CertificationMcpCollectionProgressVO.CollectionExceptionItem collectionExceptionItem(
      PracticalCollectionExceptionEntity exception,
      CertificationCandidateEntity candidate
  ) {
    return new CertificationMcpCollectionProgressVO.CollectionExceptionItem(
        id(exception.getCandidateId()),
        candidate == null ? null : candidate.getCandidateNo(),
        candidate == null ? null : candidate.getCandidateName(),
        exception.getExceptionType(),
        exception.getExceptionStatus(),
        exception.getReason(),
        exception.getDeviceStatus(),
        exception.getHandledAt(),
        exception.getCreatedAt()
    );
  }

  private List<String> collectionMissingItems(PracticalCollectionStatusEntity status) {
    List<String> items = new ArrayList<>();
    if (status == null) {
      return List.of("collectionStatusMissing", "arrivalNotRecorded", "fingerprintNotDone", "signatureNotDone");
    }
    String overall = status.getOverallStatus();
    if (!Set.of("ARRIVED", "QUEUED", "COLLECTING", "COMPLETED", "EXCEPTION_RELEASED")
        .contains(nullToEmpty(overall))) {
      items.add("arrivalNotRecorded");
    }
    if (!Set.of("DONE", "EXCEPTION_RELEASED").contains(nullToEmpty(status.getFingerprintStatus()))) {
      items.add("fingerprintNotDone");
    }
    if (!Set.of("DONE", "EXCEPTION_RELEASED").contains(nullToEmpty(status.getSignatureStatus()))) {
      items.add("signatureNotDone");
    }
    if ("PENDING_RETRY".equals(overall)) {
      items.add("pendingRetry");
    }
    if ("INFO_ERROR".equals(overall)) {
      items.add("infoError");
    }
    if ("EXCEPTION_RELEASED".equals(overall)) {
      items.add("exceptionReleased");
    }
    if (!Set.of("", "NONE", "RESOLVED").contains(nullToEmpty(status.getExceptionStatus()))) {
      items.add("collectionException:" + status.getExceptionStatus());
    }
    return items;
  }

  private boolean isCollectionCompleted(PracticalCollectionStatusEntity status) {
    return status != null && "COMPLETED".equals(status.getOverallStatus());
  }

  private boolean isPracticalCompleted(CertificationPracticalCandidateResultEntity result) {
    return result != null && "completed".equals(result.getStatus());
  }

  private CertificationMcpPracticalScoreProgressVO.UnscoredCandidate unscoredCandidate(
      CertificationCandidateEntity candidate,
      CertificationPracticalCandidateResultEntity result
  ) {
    return new CertificationMcpPracticalScoreProgressVO.UnscoredCandidate(
        id(candidate.getId()),
        candidate.getCandidateNo(),
        candidate.getCandidateName(),
        candidate.getWorkTypeName(),
        candidate.getLevelName(),
        result == null ? "pending" : result.getStatus(),
        result == null || result.getCompletedQuestionCount() == null ? 0 : result.getCompletedQuestionCount(),
        result == null || result.getQuestionCount() == null ? 0 : result.getQuestionCount()
    );
  }

  private List<CertificationMcpPracticalScoreProgressVO.AnomalyItem> practicalAnomalies(
      Long tenantId,
      Long taskId,
      List<CertificationCandidateEntity> candidates,
      Map<Long, CertificationPracticalCandidateResultEntity> resultMap
  ) {
    Map<Long, CertificationCandidateEntity> candidateMap = candidates.stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(CertificationCandidateEntity::getId, Function.identity(), (left, right) -> left));
    List<CertificationMcpPracticalScoreProgressVO.AnomalyItem> items = new ArrayList<>();
    resultMap.values().stream()
        .filter(result -> Set.of("review_required", "absent").contains(nullToEmpty(result.getStatus())))
        .forEach(result -> {
          CertificationCandidateEntity candidate = candidateMap.get(result.getCandidateId());
          items.add(new CertificationMcpPracticalScoreProgressVO.AnomalyItem(
              id(result.getCandidateId()),
              candidate == null ? null : candidate.getCandidateNo(),
              candidate == null ? null : candidate.getCandidateName(),
              "candidateResult",
              result.getStatus(),
              "Practical candidate result requires attention.",
              null,
              null,
              null,
              result.getCalculatedAt()
          ));
        });
    safeList(questionResultMapper.selectList(new LambdaQueryWrapper<CertificationPracticalQuestionResultEntity>()
            .eq(CertificationPracticalQuestionResultEntity::getTenantId, tenantId)
            .eq(CertificationPracticalQuestionResultEntity::getTaskId, taskId)
            .in(CertificationPracticalQuestionResultEntity::getStatus, List.of("review_required", "absent"))
            .eq(CertificationPracticalQuestionResultEntity::getDeleted, 0)
            .last("LIMIT " + DETAIL_LIMIT)))
        .forEach(result -> {
          CertificationCandidateEntity candidate = candidateMap.get(result.getCandidateId());
          items.add(new CertificationMcpPracticalScoreProgressVO.AnomalyItem(
              id(result.getCandidateId()),
              candidate == null ? null : candidate.getCandidateNo(),
              candidate == null ? null : candidate.getCandidateName(),
              "questionResult",
              result.getStatus(),
              "Practical question result requires attention.",
              null,
              null,
              null,
              result.getCalculatedAt()
          ));
        });
    safeList(assessorTaskMapper.selectList(new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTenantId, tenantId)
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .in(CertificationAssessorTaskEntity::getStatus, List.of("returned", "absent"))
            .eq(CertificationAssessorTaskEntity::getDeleted, 0)
            .orderByDesc(CertificationAssessorTaskEntity::getReturnedAt)
            .orderByDesc(CertificationAssessorTaskEntity::getUpdatedAt)
            .last("LIMIT " + DETAIL_LIMIT)))
        .forEach(task -> {
          CertificationCandidateEntity candidate = candidateMap.get(task.getCandidateId());
          items.add(new CertificationMcpPracticalScoreProgressVO.AnomalyItem(
              id(task.getCandidateId()),
              candidate == null ? null : candidate.getCandidateNo(),
              candidate == null ? null : candidate.getCandidateName(),
              "assessorTask",
              task.getStatus(),
              StringUtils.hasText(task.getReturnReason()) ? task.getReturnReason() : "Assessor task requires attention.",
              id(task.getId()),
              task.getTitle(),
              null,
              task.getReturnedAt() == null ? task.getUpdatedAt() : task.getReturnedAt()
          ));
        });
    return items.stream()
        .sorted(Comparator.comparing(
            CertificationMcpPracticalScoreProgressVO.AnomalyItem::occurredAt,
            Comparator.nullsLast(Comparator.reverseOrder())
        ))
        .limit(DETAIL_LIMIT)
        .toList();
  }

  private CertificationTaskEntity findTask(Long tenantId, Long taskId) {
    if (taskId == null) {
      return null;
    }
    return taskMapper.selectOne(new LambdaQueryWrapper<CertificationTaskEntity>()
        .eq(CertificationTaskEntity::getTenantId, tenantId)
        .eq(CertificationTaskEntity::getId, taskId)
        .eq(CertificationTaskEntity::getDeleted, 0)
        .last("LIMIT 1"));
  }

  private CertificationMcpTaskOverviewVO emptyOverview(Long taskId) {
    return new CertificationMcpTaskOverviewVO(id(taskId), null, null, null, null, null, "", "", null,
        0, 0, 0, 0, 0, 0, 0, 0);
  }

  private String normalizePhase(String phase) {
    if (!StringUtils.hasText(phase)) {
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

  private String summarize(List<CertificationTaskScopeEntity> scopes, Function<CertificationTaskScopeEntity, String> getter) {
    return scopes.stream()
        .map(getter)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .collect(Collectors.joining(", "));
  }

  private double percentage(long numerator, long denominator) {
    return denominator <= 0 ? 0 : Math.round(numerator * 1000.0 / denominator) / 10.0;
  }

  private Long currentTenantId() {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    return user.getTenantId();
  }

  private String id(Long value) {
    return value == null ? null : String.valueOf(value);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }
}
