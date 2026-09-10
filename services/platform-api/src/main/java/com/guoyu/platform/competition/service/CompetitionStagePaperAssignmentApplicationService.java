package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionStagePaperAssignmentBatchReq;
import com.guoyu.platform.competition.dto.CompetitionStagePaperAssignmentChangeReq;
import com.guoyu.platform.competition.dto.CompetitionStagePaperAssignmentItemReq;
import com.guoyu.platform.competition.dto.CompetitionStagePaperAssignmentQueryReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePaperAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentPlayerRow;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPaperVO;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPlayerVO;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentSummaryVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 赛段选手试卷配置用例。该关系只负责赛段级分卷，排考场次仍由理论考试排考模块负责。
 */
@Service
@Slf4j
public class CompetitionStagePaperAssignmentApplicationService {

  private static final String DRAFT = "draft";
  private static final String CONFIRMED = "confirmed";
  private static final String RANDOM = "random";
  private static final String MANUAL = "manual";
  private static final java.security.SecureRandom RANDOM_SOURCE =
      new java.security.SecureRandom();

  private final CompetitionStageApplicationService stageApplicationService;
  private final CompetitionEventService eventService;
  private final CompetitionEventWorkTypeMapper eventWorkTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionTheoryPaperSnapshotMapper paperSnapshotMapper;
  private final CompetitionStagePaperAssignmentMapper assignmentMapper;
  private final CompetitionFormalExamSessionMapper formalExamSessionMapper;

  public CompetitionStagePaperAssignmentApplicationService(
      CompetitionStageApplicationService stageApplicationService,
      CompetitionEventService eventService,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionTheoryPaperSnapshotMapper paperSnapshotMapper,
      CompetitionStagePaperAssignmentMapper assignmentMapper) {
    this(stageApplicationService, eventService, eventWorkTypeMapper, levelMapper,
        paperSnapshotMapper, assignmentMapper, null);
  }

  @Autowired
  public CompetitionStagePaperAssignmentApplicationService(
      CompetitionStageApplicationService stageApplicationService,
      CompetitionEventService eventService,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionTheoryPaperSnapshotMapper paperSnapshotMapper,
      CompetitionStagePaperAssignmentMapper assignmentMapper,
      CompetitionFormalExamSessionMapper formalExamSessionMapper) {
    this.stageApplicationService = stageApplicationService;
    this.eventService = eventService;
    this.eventWorkTypeMapper = eventWorkTypeMapper;
    this.levelMapper = levelMapper;
    this.paperSnapshotMapper = paperSnapshotMapper;
    this.assignmentMapper = assignmentMapper;
    this.formalExamSessionMapper = formalExamSessionMapper;
  }

  @Transactional(readOnly = true)
  public CompetitionStagePaperAssignmentSummaryVO summary(Long stageId) {
    AssignmentScope scope = loadScope(stageId, false);
    return summaryInternal(scope);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionStagePaperAssignmentPlayerVO> page(
      Long stageId, CompetitionStagePaperAssignmentQueryReq req) {
    if (req == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手查询条件不能为空");
    }
    AssignmentScope scope = loadScope(stageId, false);
    Page<CompetitionStagePaperAssignmentPlayerRow> page = assignmentMapper.selectEligiblePlayersPage(
        new Page<>(req.getPage(), req.getPageSize()),
        scope.tenantId(), scope.eventId(), scope.stageId(), scope.eventWorkTypeId(),
        scope.levelId(), scope.levelTexts(), normalize(req.getKeyword()), req.getOrgId(),
        req.getPaperSnapshotId(), normalize(req.getAssignmentStatus()));
    if (page == null) {
      log.warn("Stage paper assignment player page returned null, tenantId={}, stageId={}",
          scope.tenantId(), stageId);
      return PageResponse.empty(req.getPage(), req.getPageSize());
    }
    List<CompetitionStagePaperAssignmentPlayerVO> items = safe(page.getRecords()).stream()
        .map(this::toPlayerVO)
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public List<CompetitionStagePaperAssignmentPlayerVO> visualization(Long stageId) {
    AssignmentScope scope = loadScope(stageId, false);
    return eligibleRows(scope, null, null, null, null).stream()
        .map(this::toPlayerVO)
        .toList();
  }

  @Transactional
  public CompetitionStagePaperAssignmentSummaryVO autoAllocate(Long stageId) {
    AssignmentScope scope = loadScope(stageId, true);
    requireFormalExamUnlocked(scope);
    List<CompetitionTheoryPaperSnapshotEntity> papers = currentPapers(scope);
    if (papers.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前赛段暂无已发布的可分配试卷，请先完成试卷配置");
    }
    List<CompetitionStagePaperAssignmentPlayerRow> candidates =
        eligibleRows(scope, null, null, null, null);
    List<CompetitionStagePaperAssignmentEntity> existing = assignmentsForUpdate(scope);
    Map<Long, CompetitionStagePaperAssignmentEntity> existingByProfile = existing.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            CompetitionStagePaperAssignmentEntity::getContestantProfileId,
            item -> item, (left, right) -> left, LinkedHashMap::new));

    Collections.shuffle(candidates, RANDOM_SOURCE);
    for (int index = 0; index < candidates.size(); index++) {
      CompetitionStagePaperAssignmentPlayerRow candidate = candidates.get(index);
      Long paperId = papers.get(index % papers.size()).getId();
      CompetitionStagePaperAssignmentEntity entity = existingByProfile.get(
          candidate.getContestantProfileId());
      if (entity == null) {
        entity = newAssignment(scope, candidate);
        entity.setId(IdWorker.getId());
        entity.setCreatedBy(scope.userId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        existingByProfile.put(candidate.getContestantProfileId(), entity);
        applyAssignment(entity, paperId, RANDOM, DRAFT, scope.userId(), LocalDateTime.now());
        requireChanged(assignmentMapper.insert(entity), "自动分配保存失败");
        continue;
      }
      applyAssignment(entity, paperId, RANDOM, DRAFT, scope.userId(), LocalDateTime.now());
      requireChanged(assignmentMapper.updateById(entity), "自动分配保存失败");
    }
    return summaryInternal(scope);
  }

  @Transactional
  public CompetitionStagePaperAssignmentPlayerVO assign(
      Long stageId, Long contestantProfileId,
      CompetitionStagePaperAssignmentChangeReq req) {
    AssignmentScope scope = loadScope(stageId, true);
    requireFormalExamUnlocked(scope);
    Long paperSnapshotId = req == null ? null : req.paperSnapshotId();
    applyChanges(scope, List.of(new CompetitionStagePaperAssignmentItemReq(
        contestantProfileId, paperSnapshotId)), true);
    return findPlayer(scope, contestantProfileId);
  }

  @Transactional
  public CompetitionStagePaperAssignmentSummaryVO batchAssign(
      Long stageId, CompetitionStagePaperAssignmentBatchReq req) {
    AssignmentScope scope = loadScope(stageId, true);
    requireFormalExamUnlocked(scope);
    applyChanges(scope, req == null ? null : req.assignments(), true);
    return summaryInternal(scope);
  }

  @Transactional
  public CompetitionStagePaperAssignmentSummaryVO save(
      Long stageId, CompetitionStagePaperAssignmentBatchReq req) {
    AssignmentScope scope = loadScope(stageId, true);
    requireFormalExamUnlocked(scope);
    applyChanges(scope, req == null ? null : req.assignments(), false);
    return summaryInternal(scope);
  }

  @Transactional
  public CompetitionStagePaperAssignmentSummaryVO confirm(
      Long stageId, CompetitionStagePaperAssignmentBatchReq req) {
    AssignmentScope scope = loadScope(stageId, true);
    requireFormalExamUnlocked(scope);
    applyChanges(scope, req == null ? null : req.assignments(), false);
    List<CompetitionStagePaperAssignmentPlayerRow> candidates =
        eligibleRows(scope, null, null, null, null);
    if (candidates.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前赛段没有符合条件的参赛选手，不能确认配置");
    }
    List<CompetitionTheoryPaperSnapshotEntity> papers = currentPapers(scope);
    Set<Long> paperIds = papers.stream().map(CompetitionTheoryPaperSnapshotEntity::getId)
        .collect(Collectors.toSet());
    Map<Long, CompetitionStagePaperAssignmentEntity> assignments = assignmentsForUpdate(scope)
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            CompetitionStagePaperAssignmentEntity::getContestantProfileId,
            item -> item, (left, right) -> left));
    for (CompetitionStagePaperAssignmentPlayerRow candidate : candidates) {
      CompetitionStagePaperAssignmentEntity assignment = assignments.get(
          candidate.getContestantProfileId());
      if (assignment == null || assignment.getPaperSnapshotId() == null
          || !paperIds.contains(assignment.getPaperSnapshotId())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "仍有未分配试卷的选手，不能确认配置");
      }
    }
    LocalDateTime now = LocalDateTime.now();
    for (CompetitionStagePaperAssignmentPlayerRow candidate : candidates) {
      CompetitionStagePaperAssignmentEntity assignment = assignments.get(
          candidate.getContestantProfileId());
      assignment.setStatus(CONFIRMED);
      assignment.setConfirmedBy(scope.userId());
      assignment.setConfirmedAt(now);
      assignment.setUpdatedBy(scope.userId());
      assignment.setUpdatedAt(now);
      requireChanged(assignmentMapper.updateById(assignment), "确认试卷配置失败");
    }
    return summaryInternal(scope);
  }

  private CompetitionStagePaperAssignmentSummaryVO summaryInternal(AssignmentScope scope) {
    List<CompetitionTheoryPaperSnapshotEntity> papers = currentPapers(scope);
    List<CompetitionStagePaperAssignmentPlayerRow> candidates =
        eligibleRows(scope, null, null, null, null);
    Map<Long, Integer> assignedCountByPaper = new HashMap<>();
    int assignedCount = 0;
    boolean allConfirmed = !candidates.isEmpty();
    for (CompetitionStagePaperAssignmentPlayerRow candidate : candidates) {
      if (candidate.getPaperSnapshotId() != null) {
        assignedCount++;
        assignedCountByPaper.merge(candidate.getPaperSnapshotId(), 1, Integer::sum);
        if (!CONFIRMED.equals(candidate.getAssignmentStatus())) {
          allConfirmed = false;
        }
      } else {
        allConfirmed = false;
      }
    }
    List<CompetitionStagePaperAssignmentPaperVO> paperStats = papers.stream()
        .map(paper -> {
          int count = assignedCountByPaper.getOrDefault(paper.getId(), 0);
          return new CompetitionStagePaperAssignmentPaperVO(
              id(paper.getId()), paper.getPaperCode(), paper.getSourceTitle(),
              paper.getVersionNo(), count, percentage(count, candidates.size()));
        })
        .toList();
    CompetitionFormalExamSessionEntity runtime = latestFormalExamSession(scope);
    return new CompetitionStagePaperAssignmentSummaryVO(
        id(scope.event().getId()), scope.event().getName(), id(scope.stage().getId()),
        scope.stage().getName(), id(scope.workType().getId()), workTypeName(scope.workType()),
        id(scope.level().getId()), scope.level().getLevelCode(), scope.level().getLevelName(),
        candidates.size(), papers.size(), assignedCount, candidates.size() - assignedCount,
        allConfirmed, paperStats,
        runtime != null, id(runtime == null ? null : runtime.getId()),
        runtime == null ? null : runtime.getStatus());
  }

  private void requireFormalExamUnlocked(AssignmentScope scope) {
    CompetitionFormalExamSessionEntity runtime = latestFormalExamSession(scope);
    if (runtime == null) {
      return;
    }
    log.warn("Stage paper assignment rejected because formal exam is locked, tenantId={}, stageId={}, sessionId={}, status={}",
        scope.tenantId(), scope.stageId(), runtime.getId(), runtime.getStatus());
    throw new BusinessException(ErrorCode.CONFLICT,
        "正式比赛数据已生成，当前试卷分配已锁定。正式比赛将使用生成时的运行快照。");
  }

  private CompetitionFormalExamSessionEntity latestFormalExamSession(AssignmentScope scope) {
    if (formalExamSessionMapper == null) {
      return null;
    }
    return formalExamSessionMapper.selectLatestByStage(scope.tenantId(), scope.stageId());
  }

  private void applyChanges(
      AssignmentScope scope,
      List<CompetitionStagePaperAssignmentItemReq> changes,
      boolean forceDraft) {
    if (changes == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷分配内容不能为空");
    }
    List<CompetitionStagePaperAssignmentPlayerRow> candidates =
        eligibleRows(scope, null, null, null, null);
    Map<Long, CompetitionStagePaperAssignmentPlayerRow> candidateByProfile = candidates.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            CompetitionStagePaperAssignmentPlayerRow::getContestantProfileId,
            item -> item, (left, right) -> left));
    Map<Long, CompetitionTheoryPaperSnapshotEntity> papers = currentPapers(scope).stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(CompetitionTheoryPaperSnapshotEntity::getId,
            item -> item, (left, right) -> left));
    List<CompetitionStagePaperAssignmentEntity> existing = assignmentsForUpdate(scope);
    Map<Long, CompetitionStagePaperAssignmentEntity> existingByProfile = existing.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            CompetitionStagePaperAssignmentEntity::getContestantProfileId,
            item -> item, (left, right) -> left));
    Set<Long> seenProfiles = new HashSet<>();
    LocalDateTime now = LocalDateTime.now();
    for (CompetitionStagePaperAssignmentItemReq change : changes) {
      if (change == null || change.contestantProfileId() == null
          || !seenProfiles.add(change.contestantProfileId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "选手分配项不能为空且不能重复");
      }
      CompetitionStagePaperAssignmentPlayerRow candidate = candidateByProfile.get(
          change.contestantProfileId());
      if (candidate == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "只能为当前赛段符合条件的选手配置试卷");
      }
      if (change.paperSnapshotId() != null && !papers.containsKey(change.paperSnapshotId())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "只能分配当前赛段已发布的试卷");
      }
      CompetitionStagePaperAssignmentEntity entity = existingByProfile.get(
          change.contestantProfileId());
      if (entity == null) {
        entity = newAssignment(scope, candidate);
        entity.setId(IdWorker.getId());
        entity.setPaperSnapshotId(change.paperSnapshotId());
        entity.setAllocationSource(MANUAL);
        entity.setStatus(DRAFT);
        entity.setCreatedBy(scope.userId());
        entity.setUpdatedBy(scope.userId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        requireChanged(assignmentMapper.insert(entity), "保存试卷分配失败");
        existingByProfile.put(change.contestantProfileId(), entity);
        continue;
      }
      boolean paperChanged = !Objects.equals(entity.getPaperSnapshotId(), change.paperSnapshotId());
      if (!paperChanged && !forceDraft) {
        continue;
      }
      applyAssignment(entity, change.paperSnapshotId(), MANUAL, DRAFT, scope.userId(), now);
      requireChanged(assignmentMapper.updateById(entity), "保存试卷分配失败");
    }
  }

  private CompetitionStagePaperAssignmentPlayerVO findPlayer(
      AssignmentScope scope, Long contestantProfileId) {
    return eligibleRows(scope, null, null, null, null).stream()
        .filter(row -> Objects.equals(row.getContestantProfileId(), contestantProfileId))
        .findFirst()
        .map(this::toPlayerVO)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "赛段选手不存在"));
  }

  private CompetitionStagePaperAssignmentEntity newAssignment(
      AssignmentScope scope, CompetitionStagePaperAssignmentPlayerRow candidate) {
    CompetitionStagePaperAssignmentEntity entity = new CompetitionStagePaperAssignmentEntity();
    entity.setTenantId(scope.tenantId());
    entity.setEventId(scope.eventId());
    entity.setStageId(scope.stageId());
    entity.setEventWorkTypeId(scope.eventWorkTypeId());
    entity.setEventWorkTypeLevelId(scope.levelId());
    entity.setContestantProfileId(candidate.getContestantProfileId());
    entity.setContestantMemberId(candidate.getContestantMemberId());
    entity.setRegistrationId(candidate.getRegistrationId());
    return entity;
  }

  private void applyAssignment(
      CompetitionStagePaperAssignmentEntity entity,
      Long paperId,
      String source,
      String status,
      Long userId,
      LocalDateTime now) {
    entity.setPaperSnapshotId(paperId);
    entity.setAllocationSource(source);
    entity.setStatus(status);
    entity.setConfirmedBy(null);
    entity.setConfirmedAt(null);
    entity.setUpdatedBy(userId);
    entity.setUpdatedAt(now);
  }

  private List<CompetitionStagePaperAssignmentPlayerRow> eligibleRows(
      AssignmentScope scope, String keyword, Long orgId, Long paperSnapshotId,
      String assignmentStatus) {
    List<CompetitionStagePaperAssignmentPlayerRow> rows = assignmentMapper.selectEligiblePlayers(
        scope.tenantId(), scope.eventId(), scope.stageId(), scope.eventWorkTypeId(),
        scope.levelId(), scope.levelTexts(), keyword, orgId, paperSnapshotId, assignmentStatus);
    return safe(rows);
  }

  private List<CompetitionStagePaperAssignmentEntity> assignmentsForUpdate(AssignmentScope scope) {
    List<CompetitionStagePaperAssignmentEntity> rows = assignmentMapper.selectByScopeForUpdate(
        scope.tenantId(), scope.eventId(), scope.stageId(), scope.eventWorkTypeId(),
        scope.levelId());
    return safe(rows);
  }

  private List<CompetitionTheoryPaperSnapshotEntity> currentPapers(AssignmentScope scope) {
    List<CompetitionTheoryPaperSnapshotEntity> rows = paperSnapshotMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryPaperSnapshotEntity>()
            .eq(CompetitionTheoryPaperSnapshotEntity::getTenantId, scope.tenantId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getEventId, scope.eventId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getStageId, scope.stageId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getEventWorkTypeId,
                scope.eventWorkTypeId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getEventWorkTypeLevelId, scope.levelId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getStatus, "published")
            .eq(CompetitionTheoryPaperSnapshotEntity::getDeleted, 0)
            .orderByAsc(CompetitionTheoryPaperSnapshotEntity::getPaperCode)
            .orderByDesc(CompetitionTheoryPaperSnapshotEntity::getVersionNo));
    return safe(rows);
  }

  private AssignmentScope loadScope(Long stageId, boolean manage) {
    if (stageId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段不能为空");
    }
    PlatformUserPrincipal principal = currentUser();
    // 分卷工作区展示全赛事选手，查看也要求赛事管理范围，不能仅凭参赛企业身份读取他单位名单。
    CompetitionStageVO stage = stageApplicationService.assertCanManage(stageId);
    if (stage == null || stage.getEventId() == null || stage.getEventWorkTypeId() == null
        || !StringUtils.hasText(stage.getEventWorkTypeLevelCode())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前赛段未完整配置工种和技能等级");
    }
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), stage.getEventId());
    if (manage) {
      event = eventService.lockForUpdate(principal.getTenantId(), stage.getEventId());
    }
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    CompetitionEventWorkTypeEntity workType = eventWorkTypeMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventWorkTypeEntity::getEventId, stage.getEventId())
            .eq(CompetitionEventWorkTypeEntity::getId, stage.getEventWorkTypeId())
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    if (workType == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    String levelCode = stage.getEventWorkTypeLevelCode().trim();
    CompetitionEventWorkTypeLevelEntity level = levelMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, stage.getEventId())
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId,
                stage.getEventWorkTypeId())
            .eq(CompetitionEventWorkTypeLevelEntity::getLevelCode, levelCode)
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (level == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "当前赛段技能等级不存在");
    }
    List<String> levelTexts = new ArrayList<>();
    for (String text : new String[] {level.getLevelCode(), level.getLevelName(),
        level.getSourceLevelCode(), level.getSourceLevelText()}) {
      if (StringUtils.hasText(text) && !levelTexts.contains(text.trim())) {
        levelTexts.add(text.trim());
      }
    }
    return new AssignmentScope(principal, stage, event, workType, level, levelTexts);
  }

  private CompetitionStagePaperAssignmentPlayerVO toPlayerVO(
      CompetitionStagePaperAssignmentPlayerRow row) {
    boolean assigned = row.getPaperSnapshotId() != null;
    return new CompetitionStagePaperAssignmentPlayerVO(
        id(row.getContestantProfileId()), id(row.getContestantMemberId()), id(row.getRegistrationId()),
        id(row.getPlayerId()), row.getContestantNo(), row.getContestantName(),
        id(row.getOrgId()), row.getOrgName(),
        StringUtils.hasText(row.getWorkTypeName()) ? row.getWorkTypeName() : row.getRosterWorkType(),
        row.getLevelCode(), row.getLevelName(), row.getRosterLevel(), id(row.getPaperSnapshotId()),
        row.getPaperCode(), row.getPaperTitle(), row.getAllocationSource(),
        assigned ? "assigned" : "unassigned",
        CONFIRMED.equals(row.getAssignmentStatus()) && assigned ? CONFIRMED : DRAFT);
  }

  private static BigDecimal percentage(int count, int total) {
    if (total <= 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return BigDecimal.valueOf(count)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  private static String workTypeName(CompetitionEventWorkTypeEntity workType) {
    return StringUtils.hasText(workType.getDisplayName())
        ? workType.getDisplayName() : String.valueOf(workType.getWorkTypeId());
  }

  private static String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private static String id(Long value) {
    return value == null ? null : String.valueOf(value);
  }

  private static <T> List<T> safe(List<T> values) {
    return values == null ? new ArrayList<>() : values.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static void requireChanged(int affected, String message) {
    if (affected != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, message);
    }
  }

  private PlatformUserPrincipal currentUser() {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    if (principal == null) {
      log.warn("Stage paper assignment requested without authenticated user");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    return principal;
  }

  private record AssignmentScope(
      PlatformUserPrincipal principal,
      CompetitionStageVO stage,
      CompetitionEventEntity event,
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level,
      List<String> levelTexts) {

    Long tenantId() { return principal.getTenantId(); }
    Long userId() { return principal.getUserId(); }
    Long eventId() { return event.getId(); }
    Long stageId() { return stage.getId(); }
    Long eventWorkTypeId() { return workType.getId(); }
    Long levelId() { return level.getId(); }
  }
}
