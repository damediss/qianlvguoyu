package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionFormalCandidateExamQueryReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamSessionStartReq;
import com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateQuestionScoreEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateResultEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateResultStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamScoringStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamSessionStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamType;
import com.guoyu.platform.competition.enums.CompetitionStageContentType;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateQuestionScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateResultMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalPaperCandidateCountRow;
import com.guoyu.platform.competition.mapper.CompetitionFormalStatusCountRow;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentPlayerRow;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.exam.formal.FormalExamSessionFinishRules;
import com.guoyu.platform.competition.exam.formal.FormalExamSessionFinishRules.CandidateCounts;
import com.guoyu.platform.competition.exam.realtime.FormalExamConnectionStates;
import com.guoyu.platform.competition.exam.realtime.FormalExamPresenceStore;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimeProperties;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimePublisher;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateExamVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateQuestionScoreVO;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateResultVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamFinishBlockerVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPaperVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPreflightCheckVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPreflightVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamSessionVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import cn.dev33.satoken.stp.StpLogic;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 正式比赛运行域初始化。赛前选手试卷配置仍以 competition_stage_paper_assignment 为准，
 * 本服务只把已确认的「选手 → 理论试卷快照」冻结成运行实例。
 */
@Service
@Slf4j
public class CompetitionFormalExamSessionApplicationService {

  private static final long ACTIVE_OCCUPANCY = 0L;
  private static final String CONFIRMED = "confirmed";

  private final CompetitionStageApplicationService stageApplicationService;
  private final CompetitionEventService eventService;
  private final CompetitionEventWorkTypeMapper eventWorkTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionStageContentMapper stageContentMapper;
  private final CompetitionStagePaperAssignmentMapper assignmentMapper;
  private final CompetitionTheoryPaperSnapshotMapper theoryPaperMapper;
  private final CompetitionTheoryPaperQuestionMapper theoryQuestionMapper;
  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper;
  private final CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper;
  private final CompetitionFormalCandidateExamMapper candidateExamMapper;
  private final CompetitionFormalCandidateResultMapper resultMapper;
  private final CompetitionFormalCandidateQuestionScoreMapper questionScoreMapper;
  private final CompetitionFormalExamCandidateFinalizationService finalizationService;
  private final CompetitionPlayerSessionConflictDomainService sessionConflictService;
  private final StpLogic examStpLogic;
  private final CompetitionFormalExamAccessSessionService accessSessionService;
  private final Clock clock;
  private FormalExamRealtimePublisher realtimePublisher;
  private FormalExamPresenceStore presenceStore;
  private FormalExamRealtimeProperties realtimeProperties;

  public CompetitionFormalExamSessionApplicationService(
      CompetitionStageApplicationService stageApplicationService,
      CompetitionEventService eventService,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionStageContentMapper stageContentMapper,
      CompetitionStagePaperAssignmentMapper assignmentMapper,
      CompetitionTheoryPaperSnapshotMapper theoryPaperMapper,
      CompetitionTheoryPaperQuestionMapper theoryQuestionMapper,
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalCandidateResultMapper resultMapper,
      CompetitionFormalCandidateQuestionScoreMapper questionScoreMapper,
      Clock clock,
      CompetitionFormalExamCandidateFinalizationService finalizationService) {
    this(stageApplicationService, eventService, eventWorkTypeMapper, levelMapper, stageContentMapper,
        assignmentMapper, theoryPaperMapper, theoryQuestionMapper, sessionMapper, paperSnapshotMapper,
        questionSnapshotMapper, candidateExamMapper, resultMapper, questionScoreMapper, clock,
        finalizationService, null, null, null);
  }

  public CompetitionFormalExamSessionApplicationService(
      CompetitionStageApplicationService stageApplicationService,
      CompetitionEventService eventService,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionStageContentMapper stageContentMapper,
      CompetitionStagePaperAssignmentMapper assignmentMapper,
      CompetitionTheoryPaperSnapshotMapper theoryPaperMapper,
      CompetitionTheoryPaperQuestionMapper theoryQuestionMapper,
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalCandidateResultMapper resultMapper,
      CompetitionFormalCandidateQuestionScoreMapper questionScoreMapper,
      Clock clock,
      CompetitionFormalExamCandidateFinalizationService finalizationService,
      StpLogic examStpLogic) {
    this(stageApplicationService, eventService, eventWorkTypeMapper, levelMapper, stageContentMapper,
        assignmentMapper, theoryPaperMapper, theoryQuestionMapper, sessionMapper, paperSnapshotMapper,
        questionSnapshotMapper, candidateExamMapper, resultMapper, questionScoreMapper, clock,
        finalizationService, null, examStpLogic, null);
  }

  public CompetitionFormalExamSessionApplicationService(
      CompetitionStageApplicationService stageApplicationService,
      CompetitionEventService eventService,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionStageContentMapper stageContentMapper,
      CompetitionStagePaperAssignmentMapper assignmentMapper,
      CompetitionTheoryPaperSnapshotMapper theoryPaperMapper,
      CompetitionTheoryPaperQuestionMapper theoryQuestionMapper,
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalCandidateResultMapper resultMapper,
      CompetitionFormalCandidateQuestionScoreMapper questionScoreMapper,
      Clock clock,
      CompetitionFormalExamCandidateFinalizationService finalizationService,
      StpLogic examStpLogic,
      CompetitionFormalExamAccessSessionService accessSessionService) {
    this(stageApplicationService, eventService, eventWorkTypeMapper, levelMapper, stageContentMapper,
        assignmentMapper, theoryPaperMapper, theoryQuestionMapper, sessionMapper, paperSnapshotMapper,
        questionSnapshotMapper, candidateExamMapper, resultMapper, questionScoreMapper, clock,
        finalizationService, null, examStpLogic, accessSessionService);
  }

  @Autowired
  public CompetitionFormalExamSessionApplicationService(
      CompetitionStageApplicationService stageApplicationService,
      CompetitionEventService eventService,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionStageContentMapper stageContentMapper,
      CompetitionStagePaperAssignmentMapper assignmentMapper,
      CompetitionTheoryPaperSnapshotMapper theoryPaperMapper,
      CompetitionTheoryPaperQuestionMapper theoryQuestionMapper,
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalCandidateResultMapper resultMapper,
      CompetitionFormalCandidateQuestionScoreMapper questionScoreMapper,
      Clock clock,
      CompetitionFormalExamCandidateFinalizationService finalizationService,
      CompetitionPlayerSessionConflictDomainService sessionConflictService,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      CompetitionFormalExamAccessSessionService accessSessionService) {
    this.stageApplicationService = stageApplicationService;
    this.eventService = eventService;
    this.eventWorkTypeMapper = eventWorkTypeMapper;
    this.levelMapper = levelMapper;
    this.stageContentMapper = stageContentMapper;
    this.assignmentMapper = assignmentMapper;
    this.theoryPaperMapper = theoryPaperMapper;
    this.theoryQuestionMapper = theoryQuestionMapper;
    this.sessionMapper = sessionMapper;
    this.paperSnapshotMapper = paperSnapshotMapper;
    this.questionSnapshotMapper = questionSnapshotMapper;
    this.candidateExamMapper = candidateExamMapper;
    this.resultMapper = resultMapper;
    this.questionScoreMapper = questionScoreMapper;
    this.clock = clock;
    this.finalizationService = finalizationService;
    this.sessionConflictService = sessionConflictService;
    this.examStpLogic = examStpLogic;
    this.accessSessionService = accessSessionService;
  }

  @Autowired(required = false)
  public void setRealtimePublisher(FormalExamRealtimePublisher realtimePublisher) {
    this.realtimePublisher = realtimePublisher;
  }

  @Autowired(required = false)
  public void setPresenceStore(FormalExamPresenceStore presenceStore) {
    this.presenceStore = presenceStore;
  }

  @Autowired(required = false)
  public void setRealtimeProperties(FormalExamRealtimeProperties realtimeProperties) {
    this.realtimeProperties = realtimeProperties;
  }

  @Transactional
  public CompetitionFormalExamSessionVO prepareExamSession(Long stageId, String examTypeCode) {
    RuntimeScope scope = loadScope(stageId, true);
    CompetitionFormalExamType examType = requireSupportedExamType(examTypeCode);
    requireTheoryContent(scope);
    CompetitionFormalExamSessionEntity active = requireSingleActive(
        sessionMapper.selectActiveForUpdate(scope.tenantId(), scope.stageId(), examType.getCode()));
    if (active != null) {
      CompetitionFormalExamSessionStatus status =
          CompetitionFormalExamSessionStatus.fromCode(active.getStatus());
      if (status == CompetitionFormalExamSessionStatus.READY
          || status == CompetitionFormalExamSessionStatus.RUNNING) {
        assertReadyComplete(scope, active, false);
        return toSessionVO(scope, active);
      }
      if (status == CompetitionFormalExamSessionStatus.PREPARING) {
        log.warn("Rebuilding incomplete formal exam session, tenantId={}, stageId={}, sessionId={}",
            scope.tenantId(), scope.stageId(), active.getId());
        PreparedAssignments prepared = loadConfirmedAssignments(scope);
        wipeRuntimeChildren(active.getId());
        return completePreparation(scope, examType, active, prepared);
      }
      if (status == CompetitionFormalExamSessionStatus.FINISHED) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "本场正式比赛已结束，本阶段不能重新初始化正式比赛");
      }
      if (status == CompetitionFormalExamSessionStatus.CANCELLED) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "本场正式比赛已取消，本阶段不能重新初始化正式比赛");
      }
    }
    CompetitionFormalExamSessionEntity latest = sessionMapper.selectLatestByStageAndExamType(
        scope.tenantId(), scope.stageId(), examType.getCode());
    if (latest != null) {
      CompetitionFormalExamSessionStatus latestStatus =
          CompetitionFormalExamSessionStatus.fromCode(latest.getStatus());
      if (latestStatus == CompetitionFormalExamSessionStatus.FINISHED) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "本场正式比赛已结束，本阶段不能重新初始化正式比赛");
      }
      if (latestStatus == CompetitionFormalExamSessionStatus.CANCELLED) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "本场正式比赛已取消，本阶段不能重新初始化正式比赛");
      }
    }
    PreparedAssignments prepared = loadConfirmedAssignments(scope);
    CompetitionFormalExamSessionEntity session = newSession(scope, examType);
    requireChanged(sessionMapper.insert(session), "创建正式比赛运行实例失败");
    return completePreparation(scope, examType, session, prepared);
  }

  @Transactional(readOnly = true)
  public CompetitionFormalExamSessionVO getByStage(Long stageId, String examTypeCode) {
    RuntimeScope scope = loadScope(stageId, false);
    CompetitionFormalExamType examType = requireSupportedExamType(examTypeCode);
    CompetitionFormalExamSessionEntity active = requireSingleActive(
        sessionMapper.selectActive(scope.tenantId(), scope.stageId(), examType.getCode()));
    return active == null ? null : toSessionVO(scope, active);
  }

  @Transactional(readOnly = true)
  public CompetitionFormalExamSessionVO getById(Long sessionId) {
    CompetitionFormalExamSessionEntity session = requireSession(sessionId);
    RuntimeScope scope = loadScope(session.getStageId(), false);
    if (!Objects.equals(session.getEventId(), scope.eventId())
        || !Objects.equals(session.getTenantId(), scope.tenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    return toSessionVO(scope, session);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionFormalCandidateExamVO> pageCandidates(
      Long sessionId, CompetitionFormalCandidateExamQueryReq req) {
    if (req == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手查询条件不能为空");
    }
    CompetitionFormalExamSessionEntity session = requireSession(sessionId);
    loadScope(session.getStageId(), false);
    Page<CompetitionFormalCandidateExamEntity> page = candidateExamMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()),
        candidateQuery(session, req));
    if (page == null) {
      log.warn("Formal candidate exam page returned null, sessionId={}", sessionId);
      return PageResponse.empty(req.getPage(), req.getPageSize());
    }
    List<CompetitionFormalCandidateExamEntity> records = safe(page.getRecords());
    List<Long> pageIds = records.stream()
        .map(CompetitionFormalCandidateExamEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    Map<Long, CompetitionFormalCandidateResultEntity> results = resultsByCandidateIds(session.getId(), pageIds);
    Map<Long, CompetitionFormalExamAccessSessionEntity> accessByCandidate =
        accessSessionService == null
            ? Map.of()
            : accessSessionService.latestAccessByCandidateExamIds(pageIds);
    Map<Long, FormalExamPresenceStore.Presence> presenceByAccess = presenceByAccess(accessByCandidate);
    List<CompetitionFormalCandidateExamVO> items = records.stream()
        .map(item -> toCandidateVO(
            item,
            results.get(item.getId()),
            accessByCandidate.get(item.getId()),
            presenceByAccess))
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionFormalCandidateExamVO getCandidate(Long candidateExamId) {
    if (candidateExamId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手考试实例不能为空");
    }
    CompetitionFormalCandidateExamEntity candidate = candidateExamMapper.selectById(candidateExamId);
    if (candidate == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    CompetitionFormalExamSessionEntity session = requireSession(candidate.getExamSessionId());
    loadScope(session.getStageId(), false);
    if (!Objects.equals(candidate.getExamSessionId(), session.getId())
        || !Objects.equals(candidate.getTenantId(), session.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    Map<Long, CompetitionFormalExamAccessSessionEntity> access =
        accessSessionService == null
            ? Map.of()
            : accessSessionService.latestAccessByCandidateExamIds(List.of(candidate.getId()));
    return toCandidateVO(
        candidate,
        resultMapper.selectByCandidateExamId(candidate.getId()),
        access.get(candidate.getId()),
        presenceByAccess(access));
  }

  @Transactional(readOnly = true)
  public CompetitionFormalCandidateResultVO getCandidateResult(Long candidateExamId) {
    if (candidateExamId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手考试实例不能为空");
    }
    CompetitionFormalCandidateExamEntity candidate = candidateExamMapper.selectById(candidateExamId);
    if (candidate == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    CompetitionFormalExamSessionEntity session = requireSession(candidate.getExamSessionId());
    loadScope(session.getStageId(), false);
    if (!Objects.equals(candidate.getExamSessionId(), session.getId())
        || !Objects.equals(candidate.getTenantId(), session.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    CompetitionFormalCandidateResultEntity result = resultMapper.selectByCandidateExamId(candidateExamId);
    if (result == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式成绩尚未生成");
    }
    List<CompetitionFormalCandidateQuestionScoreVO> scores = List.of();
    if (CompetitionFormalCandidateResultStatus.SCORED.getCode().equals(result.getResultStatus())) {
      scores = safe(questionScoreMapper.selectByCandidateResultId(result.getId())).stream()
          .map(this::toQuestionScoreVO)
          .toList();
    }
    return toResultVO(result, scores);
  }

  @Transactional
  public CompetitionFormalCandidateExamVO forceSubmit(
      Long sessionId, Long candidateExamId, String reason) {
    if (!StringUtils.hasText(reason) || reason.trim().length() > 500) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "强制交卷原因长度为 1～500 字");
    }
    CompetitionFormalExamSessionEntity session = requireSession(sessionId);
    RuntimeScope scope = loadScope(session.getStageId(), false);
    if (!Objects.equals(session.getTenantId(), scope.tenantId())
        || !Objects.equals(session.getEventId(), scope.eventId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    CompetitionFormalCandidateExamEntity locked = candidateExamMapper.selectByIdForUpdate(candidateExamId);
    if (locked == null
        || !Objects.equals(locked.getExamSessionId(), session.getId())
        || !Objects.equals(locked.getTenantId(), session.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    CompetitionFormalCandidateExamStatus candidateStatus =
        CompetitionFormalCandidateExamStatus.fromCode(locked.getStatus());
    if (candidateStatus != null && candidateStatus.isTerminal()) {
      return toCandidateVO(locked, resultMapper.selectByCandidateExamId(locked.getId()));
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.ABSENT) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前选手已缺考，不能强制交卷");
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.WAITING_ENTER) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前选手尚未开始考试，不能强制交卷");
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.READY) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手考试状态异常，不能强制交卷");
    }
    if (candidateStatus != CompetitionFormalCandidateExamStatus.RUNNING) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前考试状态不允许强制交卷");
    }
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime deadline = deadlineTime(session);
    if (deadline != null && !now.isBefore(deadline)) {
      CompetitionFormalCandidateExamEntity autoSubmitted =
          finalizationService.autoSubmitIfRunning(locked.getId(), deadline, now);
      log.warn(
          "Formal exam force-submit after deadline used auto-submit, sessionId={}, candidateExamId={}",
          session.getId(), locked.getId());
      return toCandidateVO(autoSubmitted, resultMapper.selectByCandidateExamId(autoSubmitted.getId()));
    }
    CompetitionFormalCandidateExamEntity forced = finalizationService.forceSubmitIfRunning(
        locked.getId(), now, scope.userId(), reason.trim());
    log.warn(
        "Formal exam force-submit, operatorId={}, sessionId={}, candidateExamId={}",
        scope.userId(), session.getId(), locked.getId());
    if (realtimePublisher != null && forced != null) {
      realtimePublisher.forceSubmitCompleted(
          session.getTenantId(), session.getId(), forced.getId(), forced.getStatus());
    }
    return toCandidateVO(forced, resultMapper.selectByCandidateExamId(forced.getId()));
  }

  public void forceOffline(Long sessionId, Long candidateExamId) {
    CompetitionFormalExamSessionEntity session = requireSession(sessionId);
    RuntimeScope scope = loadScope(session.getStageId(), false);
    if (!Objects.equals(session.getTenantId(), scope.tenantId())
        || !Objects.equals(session.getEventId(), scope.eventId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    CompetitionFormalCandidateExamEntity candidate = candidateExamMapper.selectById(candidateExamId);
    if (candidate == null
        || !Objects.equals(candidate.getExamSessionId(), session.getId())
        || !Objects.equals(candidate.getTenantId(), session.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    if (candidate.getPlayerId() == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "该选手尚未绑定考试账号，无法清退");
    }
    if (accessSessionService != null) {
      accessSessionService.forceOffline(
          session.getId(),
          candidate.getId(),
          candidate.getPlayerId(),
          scope.userId(),
          CompetitionFormalExamAccessSessionService.REASON_STAFF_KICK);
      return;
    }
    if (examStpLogic == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试会话服务不可用");
    }
    examStpLogic.kickout(candidate.getPlayerId());
    log.warn(
        "Formal exam force-offline fallback kickout, operatorId={}, sessionId={}, candidateExamId={}, playerId={}",
        scope.userId(), session.getId(), candidate.getId(), candidate.getPlayerId());
  }

  @Transactional(readOnly = true)
  public CompetitionFormalExamPreflightVO getPreflight(Long sessionId) {
    CompetitionFormalExamSessionEntity session = requireSession(sessionId);
    RuntimeScope scope = loadScope(session.getStageId(), false);
    return buildPreflight(scope, session);
  }

  @Transactional
  public CompetitionFormalExamSessionVO startExamSession(
      Long sessionId, CompetitionFormalExamSessionStartReq request) {
    CompetitionFormalExamSessionEntity session = requireSessionForUpdate(sessionId);
    RuntimeScope scope = loadScope(session.getStageId(), true);
    if (!Objects.equals(session.getEventId(), scope.eventId())
        || !Objects.equals(session.getTenantId(), scope.tenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    CompetitionFormalExamSessionStatus status =
        CompetitionFormalExamSessionStatus.fromCode(session.getStatus());
    if (status == CompetitionFormalExamSessionStatus.RUNNING) {
      log.warn("Formal exam start ignored because session already running, sessionId={}",
          session.getId());
      return toSessionVO(scope, session);
    }
    if (status == CompetitionFormalExamSessionStatus.PREPARING) {
      throw new BusinessException(ErrorCode.CONFLICT, "正式比赛数据仍在准备中");
    }
    if (status == CompetitionFormalExamSessionStatus.FINISHED) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场正式比赛已结束");
    }
    if (status == CompetitionFormalExamSessionStatus.CANCELLED) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场正式比赛已取消");
    }
    if (status != CompetitionFormalExamSessionStatus.READY) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前比赛状态不允许开始");
    }

    CompetitionEventArrangementGuard.requireConfirmed(
        scope.event(), scope.workType(), scope.level());

    CompetitionFormalExamPreflightVO preflight = buildPreflight(scope, session);
    if (!preflight.ready()) {
      if (CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(session.getStatus())) {
        log.warn("Formal exam start ignored because session already running during preflight, sessionId={}",
            session.getId());
        return toSessionVO(scope, session);
      }
      String detail = preflight.checks().stream()
          .filter(item -> !Boolean.TRUE.equals(item.passed()))
          .map(CompetitionFormalExamPreflightCheckVO::message)
          .filter(StringUtils::hasText)
          .collect(Collectors.joining("；"));
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          detail.isBlank() ? "开考前检查未通过" : "开考前检查未通过：" + detail);
    }

    List<CompetitionFormalCandidateExamEntity> candidates = candidateExams(session.getId());
    int submittedLike = 0;
    for (CompetitionFormalCandidateExamEntity candidate : candidates) {
      if (isSubmitted(CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus()))) {
        submittedLike++;
      }
    }
    if (submittedLike > 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "正式比赛运行数据异常：存在已交卷状态的选手，不能开始比赛");
    }
    int readyCount = countByStatus(candidates, CompetitionFormalCandidateExamStatus.READY);
    int waitingEnterCount = countByStatus(
        candidates, CompetitionFormalCandidateExamStatus.WAITING_ENTER);
    if (readyCount <= 0) {
      CompetitionFormalExamSessionEntity latest = sessionMapper.selectById(session.getId());
      if (latest != null
          && CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(latest.getStatus())) {
        log.warn(
            "Formal exam start ignored because session already running before candidate move, sessionId={}",
            latest.getId());
        return toSessionVO(scope, latest);
      }
      throw new BusinessException(ErrorCode.CONFLICT, "当前没有已进入候考的选手，无法开始比赛");
    }
    boolean confirmWaiting = request != null
        && Boolean.TRUE.equals(request.confirmWaitingCandidates());
    if (waitingEnterCount > 0 && !confirmWaiting) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "当前仍有 " + waitingEnterCount + " 名选手未进入候考，确认后才能开始比赛");
    }
    if (session.getDurationSeconds() == null || session.getDurationSeconds() <= 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "正式比赛缺少考试时长，不能开始比赛");
    }

    LocalDateTime actualStartTime = LocalDateTime.now(clock);
    assertPlayersSessionFree(
        scope.tenantId(),
        candidates.stream().map(CompetitionFormalCandidateExamEntity::getPlayerId).toList(),
        actualStartTime,
        actualStartTime.plusSeconds(session.getDurationSeconds()),
        session.getId());
    int sessionUpdated = sessionMapper.markRunningIfReady(
        session.getId(),
        CompetitionFormalExamSessionStatus.READY.getCode(),
        CompetitionFormalExamSessionStatus.RUNNING.getCode(),
        actualStartTime,
        actualStartTime,
        scope.userId());
    if (sessionUpdated != 1) {
      CompetitionFormalExamSessionEntity latest = sessionMapper.selectByIdForUpdate(session.getId());
      if (latest != null
          && CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(latest.getStatus())) {
        log.warn("Formal exam start lost the race and reused running session, sessionId={}",
            latest.getId());
        return toSessionVO(scope, latest);
      }
      throw new BusinessException(ErrorCode.CONFLICT, "开始比赛失败，请稍后重试");
    }

    int moved = candidateExamMapper.markReadyToRunning(
        session.getId(),
        CompetitionFormalCandidateExamStatus.READY.getCode(),
        CompetitionFormalCandidateExamStatus.RUNNING.getCode(),
        actualStartTime,
        actualStartTime,
        scope.userId());
    if (moved < readyCount) {
      throw new BusinessException(ErrorCode.CONFLICT, "统一开考未能将全部候考选手切换到考试中");
    }

    CompetitionFormalExamSessionEntity started = sessionMapper.selectById(session.getId());
    if (started == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "开始比赛后未能读取运行实例");
    }
    log.warn(
        "Formal exam session started, tenantId={}, sessionId={}, actualStartTime={}, ready={}, waitingEnter={}",
        scope.tenantId(), started.getId(), actualStartTime, readyCount, waitingEnterCount);
    if (realtimePublisher != null) {
      realtimePublisher.sessionStatus(scope.tenantId(), started.getId(), started.getStatus());
      // Pads key the answering view off candidateExam.status. Bulk markReadyToRunning
      // does not emit per-candidate events, so force every local socket to HTTP-reload.
      realtimePublisher.snapshotReload(scope.tenantId(), started.getId(), "session-started");
    }
    return toSessionVO(scope, started);
  }

  private static boolean isSubmitted(CompetitionFormalCandidateExamStatus status) {
    return status != null && status.isTerminal();
  }

  private LambdaQueryWrapper<CompetitionFormalCandidateExamEntity> candidateQuery(
      CompetitionFormalExamSessionEntity session, CompetitionFormalCandidateExamQueryReq req) {
    LambdaQueryWrapper<CompetitionFormalCandidateExamEntity> wrapper =
        new LambdaQueryWrapper<CompetitionFormalCandidateExamEntity>()
            .eq(CompetitionFormalCandidateExamEntity::getTenantId, session.getTenantId())
            .eq(CompetitionFormalCandidateExamEntity::getExamSessionId, session.getId())
            .eq(CompetitionFormalCandidateExamEntity::getDeleted, 0);
    if (req.getPaperSnapshotId() != null) {
      wrapper.eq(CompetitionFormalCandidateExamEntity::getPaperSnapshotId, req.getPaperSnapshotId());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(CompetitionFormalCandidateExamEntity::getStatus, req.getStatus().trim());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      String keyword = req.getKeyword().trim();
      wrapper.and(item -> item.like(CompetitionFormalCandidateExamEntity::getCandidateNo, keyword)
          .or()
          .like(CompetitionFormalCandidateExamEntity::getCandidateName, keyword));
    }
    return wrapper
        .orderByAsc(CompetitionFormalCandidateExamEntity::getPaperCode)
        .orderByAsc(CompetitionFormalCandidateExamEntity::getCandidateNo)
        .orderByAsc(CompetitionFormalCandidateExamEntity::getId);
  }

  private CompetitionFormalExamPreflightVO buildPreflight(
      RuntimeScope scope, CompetitionFormalExamSessionEntity session) {
    List<CompetitionFormalExamPaperSnapshotEntity> papers = paperSnapshots(session.getId());
    List<CompetitionFormalExamQuestionSnapshotEntity> questions = questionSnapshots(session.getId());
    List<CompetitionFormalCandidateExamEntity> candidates = candidateExams(session.getId());
    Map<Long, CompetitionFormalExamPaperSnapshotEntity> paperById = papers.stream()
        .collect(Collectors.toMap(
            CompetitionFormalExamPaperSnapshotEntity::getId, item -> item, (left, right) -> left));
    Map<Long, Long> questionCountByPaper = questions.stream()
        .collect(Collectors.groupingBy(
            CompetitionFormalExamQuestionSnapshotEntity::getPaperSnapshotId, Collectors.counting()));
    List<CompetitionFormalExamPreflightCheckVO> checks = new ArrayList<>();
    checks.add(stagePlanConfirmationCheck(scope));
    checks.add(sessionStatusCheck(session));
    checks.add(candidateCountCheck(session, candidates));
    checks.add(candidatePaperMappingCheck(session, candidates, paperById));
    checks.add(paperSnapshotCheck(papers, questionCountByPaper));
    checks.add(questionSnapshotCheck(papers, questions));
    checks.add(candidateProfileCheck(candidates));
    boolean ready = checks.stream().allMatch(CompetitionFormalExamPreflightCheckVO::passed);
    return new CompetitionFormalExamPreflightVO(ready, checks);
  }

  private CompetitionFormalExamPreflightCheckVO stagePlanConfirmationCheck(RuntimeScope scope) {
    boolean confirmed = CompetitionEventArrangementGuard
        .isConfirmed(scope.event(), scope.workType(), scope.level());
    return new CompetitionFormalExamPreflightCheckVO(
        "STAGE_PLAN_CONFIRMATION", confirmed,
        confirmed ? "当前赛段编排已确认" : "当前赛段编排尚未确认，不能开始考试");
  }

  private CompetitionFormalExamPreflightCheckVO sessionStatusCheck(
      CompetitionFormalExamSessionEntity session) {
    if (CompetitionFormalExamSessionStatus.READY.getCode().equals(session.getStatus())) {
      return new CompetitionFormalExamPreflightCheckVO(
          "SESSION_STATUS", true, "正式比赛实例已准备完成");
    }
    if (CompetitionFormalExamSessionStatus.PREPARING.getCode().equals(session.getStatus())) {
      return new CompetitionFormalExamPreflightCheckVO(
          "SESSION_STATUS", false, "正式比赛数据仍在准备中");
    }
    if (CompetitionFormalExamSessionStatus.CANCELLED.getCode().equals(session.getStatus())) {
      return new CompetitionFormalExamPreflightCheckVO(
          "SESSION_STATUS", false, "本场正式比赛已取消");
    }
    if (CompetitionFormalExamSessionStatus.FINISHED.getCode().equals(session.getStatus())) {
      return new CompetitionFormalExamPreflightCheckVO(
          "SESSION_STATUS", false, "本场正式比赛已结束，不能再进入候考");
    }
    if (CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(session.getStatus())) {
      return new CompetitionFormalExamPreflightCheckVO(
          "SESSION_STATUS", false, "正式比赛已开始，开考前检查不再适用");
    }
    return new CompetitionFormalExamPreflightCheckVO(
        "SESSION_STATUS", false, "正式比赛实例状态不是准备完成");
  }

  private CompetitionFormalExamPreflightCheckVO candidateCountCheck(
      CompetitionFormalExamSessionEntity session,
      List<CompetitionFormalCandidateExamEntity> candidates) {
    int expected = session.getCandidateTotal() == null ? 0 : session.getCandidateTotal();
    if (expected == candidates.size() && expected > 0) {
      return new CompetitionFormalExamPreflightCheckVO(
          "CANDIDATE_COUNT", true, expected + " 名选手考试实例完整");
    }
    if (expected <= 0 && candidates.isEmpty()) {
      return new CompetitionFormalExamPreflightCheckVO(
          "CANDIDATE_COUNT", false, "尚未生成选手考试实例");
    }
    return new CompetitionFormalExamPreflightCheckVO(
        "CANDIDATE_COUNT", false,
        "选手考试实例数量异常：记录 " + expected + " 人，实际 " + candidates.size() + " 人");
  }

  private CompetitionFormalExamPreflightCheckVO candidatePaperMappingCheck(
      CompetitionFormalExamSessionEntity session,
      List<CompetitionFormalCandidateExamEntity> candidates,
      Map<Long, CompetitionFormalExamPaperSnapshotEntity> paperById) {
    int unbound = 0;
    int mismatched = 0;
    for (CompetitionFormalCandidateExamEntity candidate : candidates) {
      if (candidate.getPaperSnapshotId() == null) {
        unbound++;
        continue;
      }
      CompetitionFormalExamPaperSnapshotEntity paper = paperById.get(candidate.getPaperSnapshotId());
      if (paper == null || !Objects.equals(paper.getExamSessionId(), session.getId())) {
        mismatched++;
      }
    }
    if (unbound == 0 && mismatched == 0 && !candidates.isEmpty()) {
      return new CompetitionFormalExamPreflightCheckVO(
          "CANDIDATE_PAPER_MAPPING", true, "所有选手均已绑定正式试卷");
    }
    if (candidates.isEmpty()) {
      return new CompetitionFormalExamPreflightCheckVO(
          "CANDIDATE_PAPER_MAPPING", false, "没有可核对试卷绑定的选手考试实例");
    }
    StringBuilder message = new StringBuilder("选手正式试卷绑定异常");
    if (unbound > 0) {
      message.append("：").append(unbound).append(" 人未绑定试卷");
    }
    if (mismatched > 0) {
      message.append(unbound > 0 ? "，" : "：").append(mismatched).append(" 人指向了错误的试卷快照");
    }
    return new CompetitionFormalExamPreflightCheckVO(
        "CANDIDATE_PAPER_MAPPING", false, message.toString());
  }

  private CompetitionFormalExamPreflightCheckVO paperSnapshotCheck(
      List<CompetitionFormalExamPaperSnapshotEntity> papers,
      Map<Long, Long> questionCountByPaper) {
    if (papers.isEmpty()) {
      return new CompetitionFormalExamPreflightCheckVO(
          "PAPER_SNAPSHOT", false, "正式试卷快照缺失");
    }
    for (CompetitionFormalExamPaperSnapshotEntity paper : papers) {
      int expected = paper.getQuestionCount() == null ? 0 : paper.getQuestionCount();
      long actual = questionCountByPaper.getOrDefault(paper.getId(), 0L);
      if (expected <= 0 || actual != expected) {
        return new CompetitionFormalExamPreflightCheckVO(
            "PAPER_SNAPSHOT", false,
            displayRuntimePaper(paper) + "题目快照数量异常：预期 "
                + expected + " 题，实际 " + actual + " 题");
      }
    }
    return new CompetitionFormalExamPreflightCheckVO(
        "PAPER_SNAPSHOT", true, "正式试卷快照完整");
  }

  private CompetitionFormalExamPreflightCheckVO questionSnapshotCheck(
      List<CompetitionFormalExamPaperSnapshotEntity> papers,
      List<CompetitionFormalExamQuestionSnapshotEntity> questions) {
    if (questions.isEmpty()) {
      return new CompetitionFormalExamPreflightCheckVO(
          "QUESTION_SNAPSHOT", false, "正式题目快照缺失");
    }
    Map<Long, String> paperLabelById = papers.stream()
        .collect(Collectors.toMap(
            CompetitionFormalExamPaperSnapshotEntity::getId,
            this::displayRuntimePaper, (left, right) -> left));
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      String paperLabel = paperLabelById.getOrDefault(question.getPaperSnapshotId(), "未知试卷");
      String prefix = paperLabel + "第 " + question.getQuestionOrder() + " 题";
      if (!StringUtils.hasText(question.getQuestionType()) || !StringUtils.hasText(question.getStem())
          || question.getScore() == null) {
        return new CompetitionFormalExamPreflightCheckVO(
            "QUESTION_SNAPSHOT", false, prefix + "缺少题型、题干或分值");
      }
      String type = question.getQuestionType().trim().toLowerCase();
      if (isChoiceQuestion(type) && !hasJsonContent(question.getOptionsJson())) {
        return new CompetitionFormalExamPreflightCheckVO(
            "QUESTION_SNAPSHOT", false, prefix + "缺少选项");
      }
      if (isChoiceQuestion(type) && !hasJsonContent(question.getCorrectAnswersJson())) {
        return new CompetitionFormalExamPreflightCheckVO(
            "QUESTION_SNAPSHOT", false, prefix + "缺少标准答案");
      }
      if ("fill".equals(type) && !StringUtils.hasText(question.getFillAnswer())
          && !hasJsonContent(question.getCorrectAnswersJson())) {
        return new CompetitionFormalExamPreflightCheckVO(
            "QUESTION_SNAPSHOT", false, prefix + "缺少填空答案");
      }
    }
    return new CompetitionFormalExamPreflightCheckVO(
        "QUESTION_SNAPSHOT", true, "正式题目快照完整");
  }

  private CompetitionFormalExamPreflightCheckVO candidateProfileCheck(
      List<CompetitionFormalCandidateExamEntity> candidates) {
    int missingIdentity = 0;
    int missingName = 0;
    for (CompetitionFormalCandidateExamEntity candidate : candidates) {
      if (candidate.getContestantProfileId() == null) {
        missingIdentity++;
      }
      if (!StringUtils.hasText(candidate.getCandidateName())) {
        missingName++;
      }
    }
    if (missingIdentity == 0 && missingName == 0 && !candidates.isEmpty()) {
      return new CompetitionFormalExamPreflightCheckVO(
          "CANDIDATE_PROFILE", true, "选手基础信息完整");
    }
    if (candidates.isEmpty()) {
      return new CompetitionFormalExamPreflightCheckVO(
          "CANDIDATE_PROFILE", false, "没有可核对的选手基础信息");
    }
    if (missingIdentity > 0) {
      return new CompetitionFormalExamPreflightCheckVO(
          "CANDIDATE_PROFILE", false, missingIdentity + " 名选手缺少参赛档案");
    }
    return new CompetitionFormalExamPreflightCheckVO(
        "CANDIDATE_PROFILE", false, missingName + " 名选手缺少姓名，无法现场核对");
  }

  private CompetitionFormalExamSessionVO completePreparation(
      RuntimeScope scope,
      CompetitionFormalExamType examType,
      CompetitionFormalExamSessionEntity session,
      PreparedAssignments prepared) {
    LocalDateTime now = LocalDateTime.now();
    Integer durationSeconds = resolveDurationSeconds(scope, prepared.papers());
    LocalDateTime plannedStart = scope.stage().getStartAt();
    if (plannedStart != null && durationSeconds != null && durationSeconds > 0) {
      assertPlayersSessionFree(
          scope.tenantId(),
          prepared.players().stream()
              .map(CompetitionStagePaperAssignmentPlayerRow::getPlayerId)
              .toList(),
          plannedStart,
          plannedStart.plusSeconds(durationSeconds),
          session.getId());
    }
    Map<Long, CompetitionFormalExamPaperSnapshotEntity> paperBySourceId =
        createPaperSnapshots(scope, session, prepared, now);
    createCandidateExams(scope, session, prepared, paperBySourceId, now);
    assertReadyComplete(scope, session, true);
    session.setStatus(CompetitionFormalExamSessionStatus.READY.getCode());
    session.setCandidateTotal(prepared.players().size());
    session.setSubmittedTotal(0);
    session.setDurationSeconds(durationSeconds);
    session.setPlannedStartTime(plannedStart);
    session.setUpdatedBy(scope.userId());
    session.setUpdatedAt(now);
    requireChanged(sessionMapper.updateById(session), "更新正式比赛运行状态失败");
    log.warn("Formal exam session prepared, tenantId={}, stageId={}, sessionId={}, examType={}, candidates={}, papers={}",
        scope.tenantId(), scope.stageId(), session.getId(), examType.getCode(),
        prepared.players().size(), paperBySourceId.size());
    return toSessionVO(scope, session);
  }

  private Map<Long, CompetitionFormalExamPaperSnapshotEntity> createPaperSnapshots(
      RuntimeScope scope,
      CompetitionFormalExamSessionEntity session,
      PreparedAssignments prepared,
      LocalDateTime now) {
    Map<Long, CompetitionFormalExamPaperSnapshotEntity> created = new LinkedHashMap<>();
    for (CompetitionTheoryPaperSnapshotEntity source : prepared.papers()) {
      List<CompetitionTheoryPaperQuestionEntity> questions =
          safe(theoryQuestionMapper.selectByPaper(source.getId()));
      if (questions.isEmpty()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "试卷「" + displayPaper(source) + "」没有题目，不能初始化正式比赛");
      }
      CompetitionFormalExamPaperSnapshotEntity snapshot =
          new CompetitionFormalExamPaperSnapshotEntity();
      snapshot.setId(IdWorker.getId());
      snapshot.setTenantId(scope.tenantId());
      snapshot.setExamSessionId(session.getId());
      snapshot.setSourcePaperId(source.getId());
      snapshot.setSourceOriginPaperId(source.getSourcePaperId());
      snapshot.setScoreRuleId(source.getScoreRuleId());
      snapshot.setPaperName(source.getSourceTitle());
      snapshot.setPaperCode(source.getPaperCode());
      snapshot.setTotalScore(source.getTotalScore());
      snapshot.setQuestionCount(questions.size());
      snapshot.setDurationMinutes(source.getDurationMinutes());
      snapshot.setSnapshotVersion(source.getVersionNo() == null ? 1 : source.getVersionNo());
      snapshot.setCreatedBy(scope.userId());
      snapshot.setUpdatedBy(scope.userId());
      snapshot.setCreatedAt(now);
      snapshot.setUpdatedAt(now);
      snapshot.setDeleted(0);
      requireChanged(paperSnapshotMapper.insert(snapshot), "保存正式比赛试卷快照失败");
      int order = 1;
      for (CompetitionTheoryPaperQuestionEntity question : questions) {
        CompetitionFormalExamQuestionSnapshotEntity questionSnapshot =
            new CompetitionFormalExamQuestionSnapshotEntity();
        questionSnapshot.setId(IdWorker.getId());
        questionSnapshot.setTenantId(scope.tenantId());
        questionSnapshot.setExamSessionId(session.getId());
        questionSnapshot.setPaperSnapshotId(snapshot.getId());
        questionSnapshot.setSourceQuestionId(question.getSourceQuestionId());
        questionSnapshot.setSourcePaperQuestionId(question.getId());
        questionSnapshot.setQuestionOrder(order++);
        questionSnapshot.setQuestionType(question.getQuestionType());
        questionSnapshot.setStem(question.getContent());
        questionSnapshot.setScore(question.getScore());
        questionSnapshot.setOptionsJson(question.getOptionsJson());
        questionSnapshot.setCorrectAnswersJson(question.getCorrectAnswersJson());
        questionSnapshot.setFillAnswer(question.getFillAnswer());
        questionSnapshot.setScoringRuleCode(question.getScoringRuleCode());
        questionSnapshot.setScoringRuleVersion(question.getScoringRuleVersion());
        questionSnapshot.setCreatedBy(scope.userId());
        questionSnapshot.setUpdatedBy(scope.userId());
        questionSnapshot.setCreatedAt(now);
        questionSnapshot.setUpdatedAt(now);
        questionSnapshot.setDeleted(0);
        requireChanged(questionSnapshotMapper.insert(questionSnapshot), "保存正式比赛题目快照失败");
      }
      created.put(source.getId(), snapshot);
    }
    return created;
  }

  private void createCandidateExams(
      RuntimeScope scope,
      CompetitionFormalExamSessionEntity session,
      PreparedAssignments prepared,
      Map<Long, CompetitionFormalExamPaperSnapshotEntity> paperBySourceId,
      LocalDateTime now) {
    for (CompetitionStagePaperAssignmentPlayerRow player : prepared.players()) {
      CompetitionFormalExamPaperSnapshotEntity paper =
          paperBySourceId.get(player.getPaperSnapshotId());
      if (paper == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "选手试卷配置无法映射到正式比赛试卷快照");
      }
      CompetitionFormalCandidateExamEntity candidate = new CompetitionFormalCandidateExamEntity();
      candidate.setId(IdWorker.getId());
      candidate.setTenantId(scope.tenantId());
      candidate.setExamSessionId(session.getId());
      candidate.setContestantProfileId(player.getContestantProfileId());
      candidate.setContestantMemberId(player.getContestantMemberId());
      candidate.setRegistrationId(player.getRegistrationId());
      candidate.setPlayerId(player.getPlayerId());
      candidate.setCandidateNo(firstText(player.getContestantNo(), id(player.getPlayerId())));
      candidate.setCandidateName(player.getContestantName());
      candidate.setPaperSnapshotId(paper.getId());
      candidate.setSourcePaperId(paper.getSourcePaperId());
      candidate.setPaperCode(paper.getPaperCode());
      candidate.setPaperName(paper.getPaperName());
      candidate.setStatus(CompetitionFormalCandidateExamStatus.WAITING_ENTER.getCode());
      candidate.setAnsweredCount(0);
      candidate.setTotalQuestionCount(paper.getQuestionCount());
      candidate.setCreatedBy(scope.userId());
      candidate.setUpdatedBy(scope.userId());
      candidate.setCreatedAt(now);
      candidate.setUpdatedAt(now);
      candidate.setDeleted(0);
      requireChanged(candidateExamMapper.insert(candidate), "保存正式比赛选手实例失败");
    }
  }

  private void assertPlayersSessionFree(
      Long tenantId,
      List<Long> playerIds,
      LocalDateTime startAt,
      LocalDateTime endAt,
      Long excludeFormalExamSessionId) {
    if (sessionConflictService == null || playerIds == null || playerIds.isEmpty()) {
      return;
    }
    Set<Long> seen = new LinkedHashSet<>();
    for (Long playerId : playerIds) {
      if (playerId == null || !seen.add(playerId)) {
        continue;
      }
      sessionConflictService.assertNoOverlap(
          tenantId, playerId, startAt, endAt, null, null, excludeFormalExamSessionId);
    }
  }

  private void assertReadyComplete(
      RuntimeScope scope,
      CompetitionFormalExamSessionEntity session,
      boolean requireConfirmedSource) {
    PreparedAssignments prepared = requireConfirmedSource
        ? loadConfirmedAssignments(scope)
        : loadExistingSource(scope, session);
    List<CompetitionFormalExamPaperSnapshotEntity> papers = paperSnapshots(session.getId());
    List<CompetitionFormalExamQuestionSnapshotEntity> questions = questionSnapshots(session.getId());
    List<CompetitionFormalCandidateExamEntity> candidates = candidateExams(session.getId());
    if (papers.size() != prepared.papers().size()) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "正式比赛试卷快照数量与实际使用试卷不一致");
    }
    Map<Long, CompetitionFormalExamPaperSnapshotEntity> paperBySource = papers.stream()
        .collect(Collectors.toMap(
            CompetitionFormalExamPaperSnapshotEntity::getSourcePaperId,
            item -> item, (left, right) -> left, LinkedHashMap::new));
    Map<Long, Long> questionCountByPaper = questions.stream()
        .collect(Collectors.groupingBy(
            CompetitionFormalExamQuestionSnapshotEntity::getPaperSnapshotId, Collectors.counting()));
    for (CompetitionTheoryPaperSnapshotEntity source : prepared.papers()) {
      CompetitionFormalExamPaperSnapshotEntity snapshot = paperBySource.get(source.getId());
      if (snapshot == null) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "正式比赛缺少试卷「" + displayPaper(source) + "」的运行快照");
      }
      long actualQuestions = questionCountByPaper.getOrDefault(snapshot.getId(), 0L);
      if (actualQuestions <= 0 || actualQuestions != snapshot.getQuestionCount()) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "正式比赛试卷「" + displayPaper(source) + "」题目快照不完整");
      }
    }
    if (candidates.size() != prepared.players().size()) {
      throw new BusinessException(ErrorCode.CONFLICT, "正式比赛选手实例与赛前配置人数不一致");
    }
    Map<Long, CompetitionFormalCandidateExamEntity> candidateByProfile = candidates.stream()
        .collect(Collectors.toMap(
            CompetitionFormalCandidateExamEntity::getContestantProfileId,
            item -> item, (left, right) -> left));
    for (CompetitionStagePaperAssignmentPlayerRow player : prepared.players()) {
      CompetitionFormalCandidateExamEntity candidate =
          candidateByProfile.get(player.getContestantProfileId());
      CompetitionFormalExamPaperSnapshotEntity expectedPaper =
          paperBySource.get(player.getPaperSnapshotId());
      if (candidate == null || expectedPaper == null
          || !Objects.equals(candidate.getPaperSnapshotId(), expectedPaper.getId())
          || !Objects.equals(candidate.getSourcePaperId(), player.getPaperSnapshotId())) {
        throw new BusinessException(ErrorCode.CONFLICT, "正式比赛选手试卷映射与赛前配置不一致");
      }
    }
  }

  private PreparedAssignments loadConfirmedAssignments(RuntimeScope scope) {
    List<CompetitionStagePaperAssignmentPlayerRow> players = safe(
        assignmentMapper.selectEligiblePlayers(
            scope.tenantId(), scope.eventId(), scope.stageId(), scope.eventWorkTypeId(),
            scope.levelId(), scope.levelTexts(), null, null, null, null));
    if (players.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前赛段没有符合条件的参赛选手，不能初始化正式比赛");
    }
    assignmentMapper.selectByScopeForUpdate(
        scope.tenantId(), scope.eventId(), scope.stageId(),
        scope.eventWorkTypeId(), scope.levelId());
    Set<Long> usedPaperIds = new LinkedHashSet<>();
    for (CompetitionStagePaperAssignmentPlayerRow player : players) {
      if (player.getPaperSnapshotId() == null || !CONFIRMED.equals(player.getAssignmentStatus())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "选手试卷配置尚未全部确认，不能初始化正式比赛");
      }
      usedPaperIds.add(player.getPaperSnapshotId());
    }
    List<CompetitionTheoryPaperSnapshotEntity> papers = new ArrayList<>();
    for (Long paperId : usedPaperIds) {
      CompetitionTheoryPaperSnapshotEntity paper = theoryPaperMapper.selectById(paperId);
      if (paper == null || paper.getDeleted() != null && paper.getDeleted() != 0) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "选手已分配的试卷不存在，不能初始化正式比赛");
      }
      papers.add(paper);
    }
    return new PreparedAssignments(players, papers);
  }

  private PreparedAssignments loadExistingSource(
      RuntimeScope scope, CompetitionFormalExamSessionEntity session) {
    List<CompetitionFormalCandidateExamEntity> candidates = candidateExams(session.getId());
    List<CompetitionStagePaperAssignmentPlayerRow> players = new ArrayList<>();
    Set<Long> usedPaperIds = new LinkedHashSet<>();
    for (CompetitionFormalCandidateExamEntity candidate : candidates) {
      CompetitionStagePaperAssignmentPlayerRow row = new CompetitionStagePaperAssignmentPlayerRow();
      row.setContestantProfileId(candidate.getContestantProfileId());
      row.setPaperSnapshotId(candidate.getSourcePaperId());
      row.setAssignmentStatus(CONFIRMED);
      players.add(row);
      usedPaperIds.add(candidate.getSourcePaperId());
    }
    List<CompetitionTheoryPaperSnapshotEntity> papers = new ArrayList<>();
    for (Long paperId : usedPaperIds) {
      CompetitionTheoryPaperSnapshotEntity paper = new CompetitionTheoryPaperSnapshotEntity();
      paper.setId(paperId);
      papers.add(paper);
    }
    if (players.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "正式比赛运行数据不完整");
    }
    return new PreparedAssignments(players, papers);
  }

  private CompetitionFormalExamSessionEntity newSession(
      RuntimeScope scope, CompetitionFormalExamType examType) {
    LocalDateTime now = LocalDateTime.now();
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(IdWorker.getId());
    session.setTenantId(scope.tenantId());
    session.setEventId(scope.eventId());
    session.setStageId(scope.stageId());
    session.setEventWorkTypeId(scope.eventWorkTypeId());
    session.setEventWorkTypeLevelId(scope.levelId());
    session.setExamType(examType.getCode());
    session.setStatus(CompetitionFormalExamSessionStatus.PREPARING.getCode());
    session.setDurationSeconds(0);
    session.setPlannedStartTime(scope.stage().getStartAt());
    session.setCandidateTotal(0);
    session.setSubmittedTotal(0);
    session.setOccupancyToken(ACTIVE_OCCUPANCY);
    session.setScoringStatus(CompetitionFormalExamScoringStatus.NOT_STARTED.getCode());
    session.setCreatedBy(scope.userId());
    session.setUpdatedBy(scope.userId());
    session.setCreatedAt(now);
    session.setUpdatedAt(now);
    session.setDeleted(0);
    return session;
  }

  private void wipeRuntimeChildren(Long sessionId) {
    questionSnapshotMapper.delete(new LambdaQueryWrapper<CompetitionFormalExamQuestionSnapshotEntity>()
        .eq(CompetitionFormalExamQuestionSnapshotEntity::getExamSessionId, sessionId));
    candidateExamMapper.delete(new LambdaQueryWrapper<CompetitionFormalCandidateExamEntity>()
        .eq(CompetitionFormalCandidateExamEntity::getExamSessionId, sessionId));
    paperSnapshotMapper.delete(new LambdaQueryWrapper<CompetitionFormalExamPaperSnapshotEntity>()
        .eq(CompetitionFormalExamPaperSnapshotEntity::getExamSessionId, sessionId));
  }

  private void requireTheoryContent(RuntimeScope scope) {
    List<CompetitionStageContentEntity> contents = safe(stageContentMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageContentEntity>()
            .eq(CompetitionStageContentEntity::getTenantId, scope.tenantId())
            .eq(CompetitionStageContentEntity::getEventId, scope.eventId())
            .eq(CompetitionStageContentEntity::getStageId, scope.stageId())
            .eq(CompetitionStageContentEntity::getContentType,
                CompetitionStageContentType.THEORY.getCode())
            .eq(CompetitionStageContentEntity::getDeleted, 0)));
    if (contents.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前赛段未配置理论考试，不能初始化正式比赛");
    }
  }

  private CompetitionFormalExamType requireSupportedExamType(String examTypeCode) {
    CompetitionFormalExamType examType = CompetitionFormalExamType.fromCode(examTypeCode);
    if (examType == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的考试类型");
    }
    if (examType != CompetitionFormalExamType.THEORY) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "本阶段仅支持理论正式比赛初始化");
    }
    return examType;
  }

  private CompetitionFormalExamSessionEntity requireSession(Long sessionId) {
    if (sessionId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "正式比赛运行实例不能为空");
    }
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    return session;
  }

  private CompetitionFormalExamSessionEntity requireSessionForUpdate(Long sessionId) {
    if (sessionId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "正式比赛运行实例不能为空");
    }
    CompetitionFormalExamSessionEntity session = sessionMapper.selectByIdForUpdate(sessionId);
    if (session == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    return session;
  }

  private CompetitionFormalExamSessionEntity requireSingleActive(
      List<CompetitionFormalExamSessionEntity> rows) {
    List<CompetitionFormalExamSessionEntity> active = safe(rows);
    if (active.size() > 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段存在多个未结束的正式比赛实例");
    }
    return active.isEmpty() ? null : active.get(0);
  }

  private int resolveDurationSeconds(
      RuntimeScope scope, List<CompetitionTheoryPaperSnapshotEntity> papers) {
    int fromPapers = papers.stream()
        .map(CompetitionTheoryPaperSnapshotEntity::getDurationMinutes)
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .filter(value -> value > 0)
        .max()
        .orElse(0) * 60;
    if (fromPapers > 0) {
      return fromPapers;
    }
    if (scope.stage().getStartAt() != null && scope.stage().getEndAt() != null
        && scope.stage().getEndAt().isAfter(scope.stage().getStartAt())) {
      long seconds = Duration.between(scope.stage().getStartAt(), scope.stage().getEndAt()).getSeconds();
      if (seconds > 0 && seconds <= Integer.MAX_VALUE) {
        return (int) seconds;
      }
    }
    throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
        "正式比赛缺少考试时长，请先配置试卷时长或赛段时间");
  }

  private CompetitionFormalExamSessionVO toSessionVO(
      RuntimeScope scope, CompetitionFormalExamSessionEntity session) {
    List<CompetitionFormalExamPaperSnapshotEntity> papers = paperSnapshots(session.getId());
    Map<Long, Integer> candidateCountByPaper = paperCandidateCounts(session.getId());
    List<CompetitionFormalExamPaperVO> paperVOs = papers.stream()
        .map(paper -> new CompetitionFormalExamPaperVO(
            id(paper.getId()), id(paper.getSourcePaperId()), paper.getPaperCode(),
            paper.getPaperName(), paper.getSnapshotVersion(), paper.getQuestionCount(),
            paper.getTotalScore(), paper.getDurationMinutes(),
            candidateCountByPaper.getOrDefault(paper.getId(), 0)))
        .toList();
    CandidateCounts counts = candidateCounts(session.getId());
    LocalDateTime now = LocalDateTime.now(clock);
    List<CompetitionFormalExamFinishBlockerVO> finishBlockers =
        FormalExamSessionFinishRules.blockers(session, counts, now);
    ScoringCounts scoringCounts = scoringCounts(session, counts.total());
    String scoringStatus = StringUtils.hasText(session.getScoringStatus())
        ? session.getScoringStatus()
        : CompetitionFormalExamScoringStatus.NOT_STARTED.getCode();
    int liveSubmittedTotal = counts.submitted() + counts.autoSubmitted() + counts.forcedSubmitted();
    return new CompetitionFormalExamSessionVO(
        id(session.getId()), id(scope.event().getId()), scope.event().getName(),
        id(scope.stage().getId()), scope.stage().getName(),
        id(scope.workType().getId()), workTypeName(scope.workType()),
        id(scope.level().getId()), scope.level().getLevelCode(), scope.level().getLevelName(),
        session.getExamType(), session.getStatus(),
        session.getDurationSeconds(), session.getPlannedStartTime(),
        session.getActualStartTime(), session.getActualEndTime(),
        session.getCandidateTotal(), liveSubmittedTotal,
        counts.waiting(),
        counts.ready(),
        counts.running(),
        counts.submitted(),
        counts.autoSubmitted(),
        counts.forcedSubmitted(),
        counts.absent(),
        paperVOs.size(), paperVOs,
        now,
        deadlineTime(session),
        FormalExamSessionFinishRules.finishEligible(session, counts, now),
        finishBlockers,
        scoringStatus,
        session.getScoringStartedAt(),
        session.getScoredTime(),
        session.getScoringError(),
        scoringCounts.scored(),
        scoringCounts.absent(),
        scoringCounts.failed());
  }

  private static LocalDateTime deadlineTime(CompetitionFormalExamSessionEntity session) {
    return FormalExamSessionFinishRules.deadlineTime(session);
  }

  private static int countByStatus(
      List<CompetitionFormalCandidateExamEntity> candidates,
      CompetitionFormalCandidateExamStatus status) {
    int count = 0;
    for (CompetitionFormalCandidateExamEntity candidate : candidates) {
      if (status.getCode().equals(candidate.getStatus())) {
        count++;
      }
    }
    return count;
  }

  private static boolean isChoiceQuestion(String type) {
    return "single".equals(type) || "multi".equals(type) || "multiple".equals(type)
        || "judge".equals(type);
  }

  private static boolean hasJsonContent(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    String trimmed = value.trim();
    return !"[]".equals(trimmed) && !"{}".equals(trimmed) && !"null".equalsIgnoreCase(trimmed);
  }

  private String displayRuntimePaper(CompetitionFormalExamPaperSnapshotEntity paper) {
    if (paper == null) {
      return "未知试卷";
    }
    String code = StringUtils.hasText(paper.getPaperCode()) ? paper.getPaperCode() + "卷" : "试卷";
    return StringUtils.hasText(paper.getPaperName()) ? code + "「" + paper.getPaperName() + "」" : code;
  }

  private CompetitionFormalCandidateExamVO toCandidateVO(
      CompetitionFormalCandidateExamEntity entity,
      CompetitionFormalCandidateResultEntity result) {
    return toCandidateVO(entity, result, null, Map.of());
  }

  private CompetitionFormalCandidateExamVO toCandidateVO(
      CompetitionFormalCandidateExamEntity entity,
      CompetitionFormalCandidateResultEntity result,
      CompetitionFormalExamAccessSessionEntity access,
      Map<Long, FormalExamPresenceStore.Presence> presenceByAccess) {
    FormalExamPresenceStore.Presence presence =
        access == null || presenceByAccess == null ? null : presenceByAccess.get(access.getId());
    Instant lastSeenAt = presence == null ? null : presence.lastSeenAt();
    int window = realtimeProperties == null ? 60 : Math.max(20, realtimeProperties.getPresenceTtlSeconds());
    String connectionState = FormalExamConnectionStates.resolve(
        entity.getStatus(),
        access == null ? null : access.getStatus(),
        lastSeenAt,
        Instant.now(clock),
        window);
    return new CompetitionFormalCandidateExamVO(
        id(entity.getId()), id(entity.getExamSessionId()),
        id(entity.getContestantProfileId()), id(entity.getContestantProfileId()),
        entity.getCandidateNo(), entity.getCandidateName(),
        id(entity.getPaperSnapshotId()), id(entity.getSourcePaperId()),
        entity.getPaperCode(), entity.getPaperName(), entity.getStatus(),
        entity.getAnsweredCount(), entity.getTotalQuestionCount(),
        entity.getEnterTime(), entity.getStartTime(), entity.getSubmitTime(),
        entity.getSubmitType(), entity.getLastActiveTime(),
        entity.getForceSubmitReason(), id(entity.getForceSubmittedBy()),
        entity.getAbsentTime(), entity.getAbsentReason(),
        result == null ? null : result.getResultStatus(),
        result == null ? null : result.getTotalScore(),
        result == null ? null : result.getFullScore(),
        access == null ? null : access.getStatus(),
        connectionState,
        lastSeenAt == null ? null : lastSeenAt.toString());
  }

  private Map<Long, FormalExamPresenceStore.Presence> presenceByAccess(
      Map<Long, CompetitionFormalExamAccessSessionEntity> accessByCandidate) {
    if (presenceStore == null || accessByCandidate == null || accessByCandidate.isEmpty()) {
      return Map.of();
    }
    List<Long> accessIds = accessByCandidate.values().stream()
        .filter(Objects::nonNull)
        .map(CompetitionFormalExamAccessSessionEntity::getId)
        .filter(Objects::nonNull)
        .toList();
    Map<Long, FormalExamPresenceStore.Presence> presence = presenceStore.readMany(accessIds);
    return presence == null ? Map.of() : presence;
  }

  private CompetitionFormalCandidateResultVO toResultVO(
      CompetitionFormalCandidateResultEntity result,
      List<CompetitionFormalCandidateQuestionScoreVO> scores) {
    return new CompetitionFormalCandidateResultVO(
        id(result.getId()), id(result.getExamSessionId()), id(result.getCandidateExamId()),
        result.getResultStatus(), result.getFullScore(), result.getTotalScore(),
        result.getQuestionCount(), result.getAnsweredCount(), result.getCorrectCount(),
        result.getWrongCount(), result.getUnansweredCount(), result.getScoreVersion(),
        result.getScoredTime(), scores, result.getRevisionNo(),
        Integer.valueOf(1).equals(result.getCurrentFlag()), result.getCorrectionReason());
  }

  private CompetitionFormalCandidateQuestionScoreVO toQuestionScoreVO(
      CompetitionFormalCandidateQuestionScoreEntity score) {
    return new CompetitionFormalCandidateQuestionScoreVO(
        id(score.getQuestionSnapshotId()), score.getQuestionOrder(), score.getQuestionType(),
        score.getAnswerVersion(),
        score.getIsAnswered() != null && score.getIsAnswered() == 1,
        score.getIsCorrect() != null && score.getIsCorrect() == 1,
        score.getMaxScore(), score.getAwardedScore());
  }

  private Map<Long, CompetitionFormalCandidateResultEntity> resultsByCandidateIds(
      Long sessionId, List<Long> candidateExamIds) {
    Map<Long, CompetitionFormalCandidateResultEntity> results = new LinkedHashMap<>();
    if (candidateExamIds == null || candidateExamIds.isEmpty()) {
      return results;
    }
    for (CompetitionFormalCandidateResultEntity result : safe(
        resultMapper.selectByExamSessionIdAndCandidateIds(sessionId, candidateExamIds))) {
      if (result != null && result.getCandidateExamId() != null) {
        results.put(result.getCandidateExamId(), result);
      }
    }
    return results;
  }

  private ScoringCounts scoringCounts(
      CompetitionFormalExamSessionEntity session, int candidateTotal) {
    int scored = 0;
    int absent = 0;
    int resultTotal = 0;
    for (CompetitionFormalStatusCountRow row : safe(resultMapper.countStatusByExamSessionId(session.getId()))) {
      if (row == null) {
        continue;
      }
      resultTotal += row.totalOrZero();
      if (CompetitionFormalCandidateResultStatus.SCORED.getCode().equals(row.getStatus())) {
        scored = row.totalOrZero();
      } else if (CompetitionFormalCandidateResultStatus.ABSENT.getCode().equals(row.getStatus())) {
        absent = row.totalOrZero();
      }
    }
    String scoringStatus = session.getScoringStatus();
    int failed = 0;
    if (CompetitionFormalExamScoringStatus.FAILED.getCode().equals(scoringStatus)
        || CompetitionFormalExamScoringStatus.SCORING.getCode().equals(scoringStatus)) {
      failed = Math.max(0, candidateTotal - resultTotal);
    }
    return new ScoringCounts(scored, absent, failed);
  }

  private record ScoringCounts(int scored, int absent, int failed) {
  }

  private List<CompetitionFormalExamPaperSnapshotEntity> paperSnapshots(Long sessionId) {
    return safe(paperSnapshotMapper.selectList(
        new LambdaQueryWrapper<CompetitionFormalExamPaperSnapshotEntity>()
            .eq(CompetitionFormalExamPaperSnapshotEntity::getExamSessionId, sessionId)
            .eq(CompetitionFormalExamPaperSnapshotEntity::getDeleted, 0)
            .orderByAsc(CompetitionFormalExamPaperSnapshotEntity::getPaperCode)
            .orderByAsc(CompetitionFormalExamPaperSnapshotEntity::getId)));
  }

  private List<CompetitionFormalExamQuestionSnapshotEntity> questionSnapshots(Long sessionId) {
    return safe(questionSnapshotMapper.selectList(
        new LambdaQueryWrapper<CompetitionFormalExamQuestionSnapshotEntity>()
            .eq(CompetitionFormalExamQuestionSnapshotEntity::getExamSessionId, sessionId)
            .eq(CompetitionFormalExamQuestionSnapshotEntity::getDeleted, 0)));
  }

  private List<CompetitionFormalCandidateExamEntity> candidateExams(Long sessionId) {
    return safe(candidateExamMapper.selectList(
        new LambdaQueryWrapper<CompetitionFormalCandidateExamEntity>()
            .eq(CompetitionFormalCandidateExamEntity::getExamSessionId, sessionId)
            .eq(CompetitionFormalCandidateExamEntity::getDeleted, 0)
            .orderByAsc(CompetitionFormalCandidateExamEntity::getId)));
  }

  private CandidateCounts candidateCounts(Long sessionId) {
    int waiting = 0;
    int ready = 0;
    int running = 0;
    int submitted = 0;
    int autoSubmitted = 0;
    int forcedSubmitted = 0;
    int absent = 0;
    int total = 0;
    for (CompetitionFormalStatusCountRow row : safe(candidateExamMapper.countStatusByExamSessionId(sessionId))) {
      if (row == null) {
        continue;
      }
      int count = row.totalOrZero();
      total += count;
      CompetitionFormalCandidateExamStatus status =
          CompetitionFormalCandidateExamStatus.fromCode(row.getStatus());
      if (status == CompetitionFormalCandidateExamStatus.WAITING_ENTER) {
        waiting += count;
      } else if (status == CompetitionFormalCandidateExamStatus.READY) {
        ready += count;
      } else if (status == CompetitionFormalCandidateExamStatus.RUNNING) {
        running += count;
      } else if (status == CompetitionFormalCandidateExamStatus.SUBMITTED) {
        submitted += count;
      } else if (status == CompetitionFormalCandidateExamStatus.AUTO_SUBMITTED) {
        autoSubmitted += count;
      } else if (status == CompetitionFormalCandidateExamStatus.FORCED_SUBMITTED) {
        forcedSubmitted += count;
      } else if (status == CompetitionFormalCandidateExamStatus.ABSENT) {
        absent += count;
      }
    }
    return new CandidateCounts(
        waiting, ready, running, submitted, autoSubmitted, forcedSubmitted, absent, total);
  }

  private Map<Long, Integer> paperCandidateCounts(Long sessionId) {
    Map<Long, Integer> counts = new LinkedHashMap<>();
    for (CompetitionFormalPaperCandidateCountRow row : safe(
        candidateExamMapper.countPaperCandidatesByExamSessionId(sessionId))) {
      if (row == null || row.getPaperSnapshotId() == null) {
        continue;
      }
      counts.put(row.getPaperSnapshotId(), row.totalOrZero());
    }
    return counts;
  }

  private RuntimeScope loadScope(Long stageId, boolean manage) {
    if (stageId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段不能为空");
    }
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageVO stage = stageApplicationService.assertCanManage(stageId);
    if (stage == null || stage.getEventId() == null || stage.getEventWorkTypeId() == null
        || !StringUtils.hasText(stage.getEventWorkTypeLevelCode())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前赛段未完整配置工种和技能等级");
    }
    CompetitionEventEntity event = manage
        ? eventService.lockForUpdate(principal.getTenantId(), stage.getEventId())
        : eventService.getById(principal.getTenantId(), stage.getEventId());
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
    return new RuntimeScope(principal, stage, event, workType, level, levelTexts);
  }

  private PlatformUserPrincipal currentUser() {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    if (principal == null) {
      log.warn("Formal exam session requested without authenticated user");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    return principal;
  }

  private static String workTypeName(CompetitionEventWorkTypeEntity workType) {
    return StringUtils.hasText(workType.getDisplayName())
        ? workType.getDisplayName() : String.valueOf(workType.getWorkTypeId());
  }

  private static String displayPaper(CompetitionTheoryPaperSnapshotEntity paper) {
    if (paper == null) {
      return "未知试卷";
    }
    return StringUtils.hasText(paper.getPaperCode()) ? paper.getPaperCode() : String.valueOf(paper.getId());
  }

  private static String firstText(String left, String right) {
    if (StringUtils.hasText(left)) {
      return left.trim();
    }
    return StringUtils.hasText(right) ? right.trim() : null;
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

  private record PreparedAssignments(
      List<CompetitionStagePaperAssignmentPlayerRow> players,
      List<CompetitionTheoryPaperSnapshotEntity> papers) {
  }

  private record RuntimeScope(
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
