package com.guoyu.platform.competition.service;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSaveReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSyncItemReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSyncReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerVersionItem;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerLoginReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerSubmitReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateResultEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamSessionStatus;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
import com.guoyu.platform.competition.exam.formal.CompetitionFormalExamAccessTokens;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimePublisher;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.NormalizedAnswer;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.ProjectedOption;
import com.guoyu.platform.competition.exam.formal.FormalExamAnswerWriteRetry;
import com.guoyu.platform.competition.exam.formal.FormalExamErrorCodes;
import com.guoyu.platform.competition.exam.security.CompetitionExamPrincipal;
import com.guoyu.platform.competition.exam.security.CompetitionExamSecurityContext;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateResultMapper;
import com.guoyu.platform.competition.mapper.CompetitionTotalScoreSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.vo.CompetitionFormalExamAnswerVersionMismatchVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerAnswerSaveVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerAnswerSyncVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerCandidateVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerSubmitVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerCompetitionVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerContextVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerLoginVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerOptionVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerPaperCandidateVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerPaperMetaVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerPaperSessionVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerPaperVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerQuestionVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerSavedAnswerVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerSessionVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerStatusCandidateVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerStatusVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerResultVO;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionFormalExamPlayerApplicationService {

  private final CompetitionPlayerIdentityMapper playerMapper;
  private final CompetitionPlayerIdentityDomainService playerIdentityService;
  private final CompetitionPlayerDataHash dataHash;
  private final PasswordEncoder passwordEncoder;
  private final StpLogic examStpLogic;
  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalCandidateExamMapper candidateExamMapper;
  private final CompetitionContestantProfileMapper profileMapper;
  private final CompetitionEventService eventService;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper;
  private final CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper;
  private final CompetitionFormalCandidateAnswerMapper answerMapper;
  private final FormalExamCandidateQuestionProjection questionProjection;
  private final Clock clock;
  private final CompetitionFormalExamAccessSessionService accessSessionService;
  private FormalExamRealtimePublisher realtimePublisher;
  private CompetitionFormalExamPlayerApplicationService self = this;
  private CompetitionFormalCandidateResultMapper resultMapper;
  private CompetitionTotalScoreSnapshotMapper totalScoreSnapshotMapper;
  private CompetitionRankingSnapshotMapper rankingSnapshotMapper;
  private CompetitionRankingEntryMapper rankingEntryMapper;
  private CompetitionFormalScoringTaskService scoringTaskService;

  public static final int ANSWER_SYNC_BATCH_LIMIT = 100;

  public CompetitionFormalExamPlayerApplicationService(
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionPlayerIdentityDomainService playerIdentityService,
      CompetitionPlayerDataHash dataHash,
      PasswordEncoder passwordEncoder,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionContestantProfileMapper profileMapper,
      CompetitionEventService eventService,
      CompetitionStageMapper stageMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateAnswerMapper answerMapper,
      FormalExamCandidateQuestionProjection questionProjection,
      Clock clock,
      CompetitionFormalExamAccessSessionService accessSessionService) {
    this.playerMapper = playerMapper;
    this.playerIdentityService = playerIdentityService;
    this.dataHash = dataHash;
    this.passwordEncoder = passwordEncoder;
    this.examStpLogic = examStpLogic;
    this.sessionMapper = sessionMapper;
    this.candidateExamMapper = candidateExamMapper;
    this.profileMapper = profileMapper;
    this.eventService = eventService;
    this.stageMapper = stageMapper;
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
    this.paperSnapshotMapper = paperSnapshotMapper;
    this.questionSnapshotMapper = questionSnapshotMapper;
    this.answerMapper = answerMapper;
    this.questionProjection = questionProjection;
    this.clock = clock;
    this.accessSessionService = accessSessionService;
  }

  @Autowired(required = false)
  public void setRealtimePublisher(FormalExamRealtimePublisher realtimePublisher) {
    this.realtimePublisher = realtimePublisher;
  }

  @Autowired
  @Lazy
  public void setSelf(CompetitionFormalExamPlayerApplicationService self) {
    this.self = self;
  }

  @Autowired(required = false)
  public void setPlayerResultDependencies(
      CompetitionFormalCandidateResultMapper resultMapper,
      CompetitionTotalScoreSnapshotMapper totalScoreSnapshotMapper,
      CompetitionRankingSnapshotMapper rankingSnapshotMapper,
      CompetitionRankingEntryMapper rankingEntryMapper) {
    this.resultMapper = resultMapper;
    this.totalScoreSnapshotMapper = totalScoreSnapshotMapper;
    this.rankingSnapshotMapper = rankingSnapshotMapper;
    this.rankingEntryMapper = rankingEntryMapper;
  }

  @Autowired(required = false)
  public void setScoringTaskService(CompetitionFormalScoringTaskService scoringTaskService) {
    this.scoringTaskService = scoringTaskService;
  }

  @Transactional(readOnly = true)
  public CompetitionFormalExamPlayerResultVO result(String sessionIdValue) {
    ResolvedPlayerExam resolved = resolve(currentPlayer(), parseSessionId(sessionIdValue), false);
    CompetitionFormalCandidateExamEntity candidate = resolved.candidateExam();
    CompetitionFormalCandidateResultEntity formalResult =
        resultMapper == null ? null : resultMapper.selectByCandidateExamId(candidate.getId());
    if (formalResult == null) return CompetitionFormalExamPlayerResultVO.pending("scoring");
    CompetitionFormalExamSessionEntity session = resolved.session();
    CompetitionTotalScoreSnapshotEntity total = totalScoreSnapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTotalScoreSnapshotEntity>()
            .eq(CompetitionTotalScoreSnapshotEntity::getEventId, session.getEventId())
            .eq(CompetitionTotalScoreSnapshotEntity::getStageId, session.getStageId())
            .eq(CompetitionTotalScoreSnapshotEntity::getEventWorkTypeId, session.getEventWorkTypeId())
            .eq(session.getEventWorkTypeLevelId() != null,
                CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId,
                session.getEventWorkTypeLevelId())
            .isNull(session.getEventWorkTypeLevelId() == null,
                CompetitionTotalScoreSnapshotEntity::getEventWorkTypeLevelId)
            .eq(CompetitionTotalScoreSnapshotEntity::getContestantMemberId,
                candidate.getContestantMemberId())
            .eq(CompetitionTotalScoreSnapshotEntity::getCurrentFlag, 1)
            .eq(CompetitionTotalScoreSnapshotEntity::getConfirmationStatus, "confirmed")
            .eq(CompetitionTotalScoreSnapshotEntity::getDeleted, 0)
            .orderByDesc(CompetitionTotalScoreSnapshotEntity::getCalculatedAt)
            .last("LIMIT 1"));
    CompetitionRankingSnapshotEntity ranking = rankingSnapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
            .eq(CompetitionRankingSnapshotEntity::getEventId, session.getEventId())
            .eq(CompetitionRankingSnapshotEntity::getStageId, session.getStageId())
            .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, session.getEventWorkTypeId())
            .eq(session.getEventWorkTypeLevelId() != null,
                CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId,
                session.getEventWorkTypeLevelId())
            .isNull(session.getEventWorkTypeLevelId() == null,
                CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
            .eq(CompetitionRankingSnapshotEntity::getStatus, "published")
            .eq(CompetitionRankingSnapshotEntity::getDeleted, 0)
            .orderByDesc(CompetitionRankingSnapshotEntity::getPublishedAt)
            .orderByDesc(CompetitionRankingSnapshotEntity::getSnapshotVersion)
            .last("LIMIT 1"));
    if (ranking == null) {
      if (total == null && !"absent".equals(formalResult.getResultStatus())) {
        return CompetitionFormalExamPlayerResultVO.pending("awaiting_sources");
      }
      CompetitionRankingSnapshotEntity pending = rankingSnapshotMapper.selectOne(
          new LambdaQueryWrapper<CompetitionRankingSnapshotEntity>()
              .eq(CompetitionRankingSnapshotEntity::getEventId, session.getEventId())
              .eq(CompetitionRankingSnapshotEntity::getStageId, session.getStageId())
              .eq(CompetitionRankingSnapshotEntity::getEventWorkTypeId, session.getEventWorkTypeId())
              .eq(session.getEventWorkTypeLevelId() != null,
                  CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId,
                  session.getEventWorkTypeLevelId())
              .isNull(session.getEventWorkTypeLevelId() == null,
                  CompetitionRankingSnapshotEntity::getEventWorkTypeLevelId)
              .in(CompetitionRankingSnapshotEntity::getStatus, "draft", "confirmed")
              .eq(CompetitionRankingSnapshotEntity::getDeleted, 0)
              .orderByDesc(CompetitionRankingSnapshotEntity::getSnapshotVersion).last("LIMIT 1"));
      return CompetitionFormalExamPlayerResultVO.pending(
          pending == null ? "awaiting_review" :
              ("confirmed".equals(pending.getStatus()) ? "unpublished" : "awaiting_review"));
    }
    CompetitionRankingEntryEntity entry = rankingEntryMapper.selectOne(
        new LambdaQueryWrapper<CompetitionRankingEntryEntity>()
            .eq(CompetitionRankingEntryEntity::getRankingSnapshotId, ranking.getId())
            .eq(CompetitionRankingEntryEntity::getContestantMemberId,
                candidate.getContestantMemberId())
            .eq(CompetitionRankingEntryEntity::getDeleted, 0).last("LIMIT 1"));
    if (entry == null) return CompetitionFormalExamPlayerResultVO.pending("unpublished");
    if (entry.getTotalScoreSnapshotId() != null) {
      CompetitionTotalScoreSnapshotEntity publishedTotal =
          totalScoreSnapshotMapper.selectById(entry.getTotalScoreSnapshotId());
      if (publishedTotal == null
          || !candidate.getContestantMemberId().equals(publishedTotal.getContestantMemberId())) {
        return CompetitionFormalExamPlayerResultVO.pending("unpublished");
      }
      total = publishedTotal;
    }
    return new CompetitionFormalExamPlayerResultVO("published",
        total == null ? BigDecimal.ZERO : total.getTheoryScore(),
        total == null ? BigDecimal.ZERO : total.getSimulationScore(),
        total == null ? BigDecimal.ZERO : total.getPracticalScore(),
        total == null ? BigDecimal.ZERO : total.getTotalScore(), entry.getDisplayRank(),
        Integer.valueOf(1).equals(entry.getTiedFlag()), ranking.getPublishedAt());
  }

  @Transactional
  public CompetitionFormalExamPlayerLoginVO login(CompetitionFormalExamPlayerLoginReq request) {
    CompetitionPlayerIdentityEntity player = authenticateActivePlayer(request);
    Long sessionId = parseOptionalSessionId(request.sessionId());
    if (accessSessionService == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试会话服务不可用");
    }
    Long previousTenant = TenantContextHolder.getTenantId();
    if (previousTenant != null && !previousTenant.equals(player.getTenantId())) {
      log.warn("Formal exam login rejected: tenant header mismatch, playerId={}", player.getId());
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    try {
      TenantContextHolder.setTenantId(player.getTenantId());
      Long resolvedSessionId = resolveLoginSession(player, sessionId);
      CompetitionFormalExamAccessSessionService.IssuedAccess issued =
          accessSessionService.issueLogin(player, resolvedSessionId, request.deviceId());
      SaLoginParameter loginParameter = SaLoginParameter.create()
          .setIsConcurrent(true)
          .setIsShare(false)
          .setDevice(CompetitionFormalExamAccessTokens.device(issued.accessSessionId()));
      loginParameter.setExtra("accessSessionId", String.valueOf(issued.accessSessionId()));
      loginParameter.setExtra("generation", String.valueOf(issued.generation()));
      examStpLogic.login(player.getId(), loginParameter);
      String token = examStpLogic.getTokenValue();
      if (token == null || token.isBlank()) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试会话创建失败");
      }
      accessSessionService.attachTokenFingerprint(issued.accessSessionId(), player.getId(), token);
      return new CompetitionFormalExamPlayerLoginVO(
          id(player.getId()),
          id(player.getTenantId()),
          token,
          "Bearer",
          examStpLogic.getTokenTimeout(),
          id(issued.accessSessionId()),
          issued.generation(),
          issued.deviceId(),
          id(issued.examSessionId()),
          id(issued.candidateExamId()));
    } finally {
      if (previousTenant == null) {
        TenantContextHolder.clear();
      } else {
        TenantContextHolder.setTenantId(previousTenant);
      }
    }
  }

  public void logout() {
    CompetitionExamPrincipal principal = currentPlayer();
    if (accessSessionService != null) {
      accessSessionService.logout(principal);
      return;
    }
    examStpLogic.logout(principal.getPlayerId());
  }

  @Transactional(readOnly = true)
  public CompetitionFormalExamPlayerContextVO context(String sessionIdValue) {
    return toContext(resolve(currentPlayer(), parseSessionId(sessionIdValue), false));
  }

  @Transactional(readOnly = true)
  public CompetitionFormalExamPlayerStatusVO status(String sessionIdValue) {
    return toStatus(resolveStatus(currentPlayer(), parseSessionId(sessionIdValue)));
  }

  @Transactional
  public CompetitionFormalExamPlayerContextVO enter(String sessionIdValue) {
    CompetitionExamPrincipal principal = currentPlayer();
    Long sessionId = parseSessionId(sessionIdValue);
    ResolvedPlayerExam resolved = resolve(principal, sessionId, true);
    CompetitionFormalCandidateExamStatus candidateStatus =
        CompetitionFormalCandidateExamStatus.fromCode(resolved.candidateExam().getStatus());
    if (candidateStatus == CompetitionFormalCandidateExamStatus.READY) {
      return toContext(resolved);
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.RUNNING) {
      return toContext(resolved);
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.ABSENT) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场考试已记录为缺考，不能进入候考");
    }
    if (isSubmitted(candidateStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场考试已经交卷，不能重新进入候考");
    }
    if (candidateStatus != CompetitionFormalCandidateExamStatus.WAITING_ENTER) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前考试状态不允许进入候考");
    }
    assertSessionAllowsFirstEnter(resolved.session());
    LocalDateTime now = LocalDateTime.now(clock);
    int updated = candidateExamMapper.markEnterIfWaiting(
        resolved.candidateExam().getId(),
        CompetitionFormalCandidateExamStatus.WAITING_ENTER.getCode(),
        CompetitionFormalCandidateExamStatus.READY.getCode(),
        now, now, principal.getPlayerId());
    if (updated == 1) {
      CompetitionFormalCandidateExamEntity entered =
          candidateExamMapper.selectById(resolved.candidateExam().getId());
      if (entered == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "进入比赛后未能读取考试实例");
      }
      publishCandidateStatus(principal, sessionId, entered.getId(), "ready", entered.getStatus());
      return toContext(new ResolvedPlayerExam(
          resolved.session(), resolved.profile(), entered,
          resolved.event(), resolved.stage(), resolved.workType(), resolved.level()));
    }
    CompetitionFormalCandidateExamEntity latest =
        candidateExamMapper.selectByIdForUpdate(resolved.candidateExam().getId());
    if (latest == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "进入比赛后未能读取考试实例");
    }
    CompetitionFormalCandidateExamStatus latestStatus =
        CompetitionFormalCandidateExamStatus.fromCode(latest.getStatus());
    if (latestStatus == CompetitionFormalCandidateExamStatus.READY
        || latestStatus == CompetitionFormalCandidateExamStatus.RUNNING) {
      return toContext(new ResolvedPlayerExam(
          resolved.session(), resolved.profile(), latest,
          resolved.event(), resolved.stage(), resolved.workType(), resolved.level()));
    }
    if (isSubmitted(latestStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场考试已经交卷，不能重新进入候考");
    }
    if (latestStatus == CompetitionFormalCandidateExamStatus.ABSENT) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场考试已记录为缺考，不能进入候考");
    }
    throw new BusinessException(ErrorCode.CONFLICT, "进入比赛失败，请稍后重试");
  }

  @Transactional(readOnly = true)
  public CompetitionFormalExamPlayerPaperVO paper(String sessionIdValue) {
    ResolvedPlayerExam resolved = resolve(currentPlayer(), parseSessionId(sessionIdValue), false);
    assertCanLoadPaper(resolved);
    return toPaper(resolved, LocalDateTime.now(clock));
  }

  public CompetitionFormalExamPlayerAnswerSaveVO saveAnswer(
      String sessionIdValue, String questionSnapshotIdValue, CompetitionFormalExamPlayerAnswerSaveReq request) {
    return FormalExamAnswerWriteRetry.execute(
        () -> self.saveAnswerOnce(sessionIdValue, questionSnapshotIdValue, request));
  }

  public CompetitionFormalExamPlayerAnswerSyncVO syncAnswers(
      String sessionIdValue, CompetitionFormalExamPlayerAnswerSyncReq request) {
    List<CompetitionFormalExamPlayerAnswerSyncItemReq> items =
        request == null || request.answers() == null ? List.of() : request.answers();
    if (items.size() > ANSWER_SYNC_BATCH_LIMIT) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "单次同步题目数量不能超过" + ANSWER_SYNC_BATCH_LIMIT);
    }
    if (items.isEmpty()) {
      return new CompetitionFormalExamPlayerAnswerSyncVO(List.of(), LocalDateTime.now(clock));
    }
    return FormalExamAnswerWriteRetry.execute(
        () -> self.syncAnswersOnce(sessionIdValue, request));
  }

  @Transactional
  public CompetitionFormalExamPlayerAnswerSaveVO saveAnswerOnce(
      String sessionIdValue, String questionSnapshotIdValue, CompetitionFormalExamPlayerAnswerSaveReq request) {
    return saveAnswerInCurrentTransaction(sessionIdValue, questionSnapshotIdValue, request);
  }

  @Transactional
  public CompetitionFormalExamPlayerAnswerSyncVO syncAnswersOnce(
      String sessionIdValue, CompetitionFormalExamPlayerAnswerSyncReq request) {
    List<CompetitionFormalExamPlayerAnswerSyncItemReq> items =
        request == null || request.answers() == null ? List.of() : request.answers();
    if (items.size() > ANSWER_SYNC_BATCH_LIMIT) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "单次同步题目数量不能超过" + ANSWER_SYNC_BATCH_LIMIT);
    }
    LocalDateTime now = LocalDateTime.now(clock);
    List<CompetitionFormalExamPlayerAnswerSaveVO> results = new ArrayList<>();
    if (items.isEmpty()) {
      return new CompetitionFormalExamPlayerAnswerSyncVO(results, now);
    }
    CompetitionExamPrincipal principal;
    ResolvedPlayerExam resolved;
    try {
      principal = currentPlayer();
      resolved = resolve(principal, parseSessionId(sessionIdValue), true);
      assertCanSaveAnswer(resolved);
      assertBeforeDeadline(resolved.session(), now);
    } catch (BusinessException ex) {
      for (CompetitionFormalExamPlayerAnswerSyncItemReq item : items) {
        if (item != null) {
          results.add(rejectedSave(item, ex));
        }
      }
      return new CompetitionFormalExamPlayerAnswerSyncVO(results, now);
    }

    LinkedHashSet<Long> questionIds = new LinkedHashSet<>();
    Set<Long> duplicateQuestionIds = new LinkedHashSet<>();
    List<BatchSyncItem> work = new ArrayList<>();
    for (CompetitionFormalExamPlayerAnswerSyncItemReq item : items) {
      if (item == null) {
        continue;
      }
      try {
        Long questionId = parseQuestionSnapshotId(item.questionSnapshotId());
        if (!questionIds.add(questionId)) {
          duplicateQuestionIds.add(questionId);
        }
        work.add(new BatchSyncItem(item, questionId, null));
      } catch (BusinessException ex) {
        work.add(new BatchSyncItem(item, null, ex));
      }
    }

    List<Long> orderedQuestionIds = new ArrayList<>(questionIds);
    orderedQuestionIds.sort(Long::compareTo);
    Map<Long, CompetitionFormalExamQuestionSnapshotEntity> questionsById = new LinkedHashMap<>();
    Map<Long, CompetitionFormalCandidateAnswerEntity> answersByQuestion = new LinkedHashMap<>();
    if (!orderedQuestionIds.isEmpty() && resolved.candidateExam().getPaperSnapshotId() != null) {
      for (CompetitionFormalExamQuestionSnapshotEntity question : safe(
          questionSnapshotMapper.selectByPaperSnapshotIdAndIds(
              resolved.candidateExam().getPaperSnapshotId(), orderedQuestionIds))) {
        questionsById.put(question.getId(), question);
      }
      List<CompetitionFormalCandidateAnswerEntity> existing = safe(
          answerMapper.selectByCandidateQuestionIds(
              resolved.candidateExam().getId(), orderedQuestionIds));
      for (CompetitionFormalCandidateAnswerEntity answer : existing) {
        answersByQuestion.put(answer.getQuestionSnapshotId(), answer);
      }
    }

    List<CompetitionFormalCandidateAnswerEntity> toInsert = new ArrayList<>();
    List<CompetitionFormalCandidateAnswerEntity> toUpdate = new ArrayList<>();
    boolean recount = false;
    for (BatchSyncItem workItem : work) {
      if (workItem.error() != null) {
        results.add(rejectedSave(workItem.item(), workItem.error()));
        continue;
      }
      if (duplicateQuestionIds.contains(workItem.questionId())
          && !isFirstOccurrence(work, workItem)) {
        results.add(rejectedSave(workItem.item(),
            new BusinessException(ErrorCode.BAD_REQUEST, "同步清单存在重复题目")));
        continue;
      }
      CompetitionFormalExamQuestionSnapshotEntity question = questionsById.get(workItem.questionId());
      if (!belongsToCandidatePaper(resolved.session(), resolved.candidateExam(), question)) {
        log.warn("Formal exam cross-paper answer rejected, playerId={}, questionSnapshotId={}",
            principal.getPlayerId(), workItem.questionId());
        results.add(rejectedSave(workItem.item(),
            new BusinessException(ErrorCode.FORBIDDEN, "题目不属于当前试卷")));
        continue;
      }
      CompetitionFormalExamPlayerAnswerSaveReq saveReq =
          new CompetitionFormalExamPlayerAnswerSaveReq(
              workItem.item().answer(), workItem.item().marked(), workItem.item().version());
      if (saveReq.version() == null || saveReq.version() < 1) {
        results.add(rejectedSave(workItem.item(),
            new BusinessException(ErrorCode.BAD_REQUEST, "答案版本无效")));
        continue;
      }
      try {
        AnswerMutation mutation = evaluateAnswer(
            resolved, question, answersByQuestion.get(workItem.questionId()),
            saveReq, now, principal.getPlayerId());
        results.add(mutation.vo());
        if (mutation.insert() != null) {
          toInsert.add(mutation.insert());
          answersByQuestion.put(workItem.questionId(), mutation.insert());
          recount = true;
        } else if (mutation.update() != null) {
          toUpdate.add(mutation.update());
          answersByQuestion.put(workItem.questionId(), mutation.update());
          recount = true;
        }
      } catch (BusinessException ex) {
        results.add(rejectedSave(workItem.item(), ex));
      }
    }

    toInsert.sort(Comparator.comparing(
        CompetitionFormalCandidateAnswerEntity::getQuestionSnapshotId,
        Comparator.nullsLast(Long::compareTo)));
    toUpdate.sort(Comparator.comparing(
        CompetitionFormalCandidateAnswerEntity::getQuestionSnapshotId,
        Comparator.nullsLast(Long::compareTo)));
    for (CompetitionFormalCandidateAnswerEntity created : toInsert) {
      requireChanged(answerMapper.insert(created), "保存答案失败");
    }
    for (CompetitionFormalCandidateAnswerEntity updated : toUpdate) {
      requireChanged(answerMapper.updateById(updated), "保存答案失败");
    }
    int answeredCount = recount
        ? refreshAnsweredCount(resolved.candidateExam().getId(), now, principal.getPlayerId())
        : answeredCountOf(resolved.candidateExam());
    if (recount && realtimePublisher != null) {
      realtimePublisher.progress(
          principal.getTenantId(),
          resolved.session().getId(),
          resolved.candidateExam().getId(),
          principal.getAccessSessionId(),
          principal.getGeneration(),
          answeredCount,
          resolved.candidateExam().getTotalQuestionCount());
    }
    if (recount) {
      for (int i = 0; i < results.size(); i++) {
        CompetitionFormalExamPlayerAnswerSaveVO vo = results.get(i);
        if (vo.accepted()) {
          results.set(i, new CompetitionFormalExamPlayerAnswerSaveVO(
              vo.questionSnapshotId(), vo.version(), vo.isAnswered(), vo.marked(),
              vo.savedAt(), answeredCount, vo.serverTime(), vo.accepted(),
              vo.conflictCode(), vo.answer(), vo.status()));
        }
      }
    }
    return new CompetitionFormalExamPlayerAnswerSyncVO(results, now);
  }

  @Transactional
  public CompetitionFormalExamPlayerSubmitVO submit(
      String sessionIdValue, CompetitionFormalExamPlayerSubmitReq request) {
    CompetitionExamPrincipal principal = currentPlayer();
    Long sessionId = parseSessionId(sessionIdValue);
    LocalDateTime now = LocalDateTime.now(clock);
    ResolvedPlayerExam resolved = resolve(principal, sessionId, true);
    CompetitionFormalCandidateExamEntity candidate = resolved.candidateExam();
    CompetitionFormalCandidateExamStatus candidateStatus =
        CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus());
    if (candidateStatus == CompetitionFormalCandidateExamStatus.SUBMITTED) {
      return toSubmitVo(resolved, candidate, now);
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.AUTO_SUBMITTED
        || candidateStatus == CompetitionFormalCandidateExamStatus.FORCED_SUBMITTED) {
      return toSubmitVo(resolved, candidate, now);
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.ABSENT) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场考试已记录为缺考，不能交卷");
    }

    CompetitionFormalExamSessionStatus sessionStatus =
        CompetitionFormalExamSessionStatus.fromCode(resolved.session().getStatus());
    if (sessionStatus != CompetitionFormalExamSessionStatus.RUNNING) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前比赛状态不允许交卷");
    }
    if (candidateStatus != CompetitionFormalCandidateExamStatus.RUNNING) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前考试状态不允许交卷");
    }
    assertBeforeDeadline(resolved.session(), now);

    List<CompetitionFormalExamQuestionSnapshotEntity> questions = paperQuestions(resolved);
    Map<Long, Integer> clientVersions = clientAnswerVersions(request, questions);
    Map<Long, Integer> serverVersions = serverAnswerVersions(candidate.getId(), questions);
    List<CompetitionFormalExamAnswerVersionMismatchVO> mismatches = new ArrayList<>();
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      int clientVersion = clientVersions.getOrDefault(question.getId(), 0);
      int serverVersion = serverVersions.getOrDefault(question.getId(), 0);
      if (clientVersion != serverVersion) {
        mismatches.add(new CompetitionFormalExamAnswerVersionMismatchVO(
            id(question.getId()), clientVersion, serverVersion));
      }
    }
    if (!mismatches.isEmpty()) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("questions", mismatches);
      throw new BusinessException(
          FormalExamErrorCodes.ANSWERS_NOT_SYNCED, "部分答案尚未完成服务器同步", data);
    }

    int answeredCount = answerMapper.countAnswered(candidate.getId());
    int totalQuestionCount = questions.size();
    int unansweredCount = Math.max(0, totalQuestionCount - answeredCount);
    if (unansweredCount > 0 && !Boolean.TRUE.equals(request == null ? null : request.confirmUnanswered())) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("unansweredCount", unansweredCount);
      data.put("answeredCount", answeredCount);
      data.put("totalQuestionCount", totalQuestionCount);
      throw new BusinessException(
          FormalExamErrorCodes.UNANSWERED_CONFIRM_REQUIRED, "当前还有未作答题目，请确认后再交卷", data);
    }

    int updated = candidateExamMapper.markSubmittedIfRunning(
        candidate.getId(),
        CompetitionFormalCandidateExamStatus.RUNNING.getCode(),
        CompetitionFormalCandidateExamStatus.SUBMITTED.getCode(),
        now,
        FormalExamErrorCodes.SUBMIT_TYPE_MANUAL,
        answeredCount,
        now,
        principal.getPlayerId());
    if (updated == 1) {
      CompetitionFormalCandidateExamEntity submitted = candidateExamMapper.selectById(candidate.getId());
      if (submitted == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "交卷后未能读取考试实例");
      }
      log.warn("Formal exam manual submit, playerId={}, sessionId={}, candidateExamId={}, answered={}/{}",
          principal.getPlayerId(), sessionId, candidate.getId(), answeredCount, totalQuestionCount);
      if (scoringTaskService != null) {
        scoringTaskService.enqueueInitial(submitted, "manual-submit", principal.getPlayerId());
      }
      publishCandidateStatus(principal, sessionId, submitted.getId(), "running", submitted.getStatus());
      return toSubmitVo(submitted, now, answeredCount, totalQuestionCount, unansweredCount);
    }

    CompetitionFormalCandidateExamEntity latest =
        candidateExamMapper.selectByIdForUpdate(candidate.getId());
    if (latest == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "交卷后未能读取考试实例");
    }
    CompetitionFormalCandidateExamStatus latestStatus =
        CompetitionFormalCandidateExamStatus.fromCode(latest.getStatus());
    if (latestStatus == CompetitionFormalCandidateExamStatus.SUBMITTED
        || latestStatus == CompetitionFormalCandidateExamStatus.AUTO_SUBMITTED
        || latestStatus == CompetitionFormalCandidateExamStatus.FORCED_SUBMITTED) {
      return toSubmitVo(new ResolvedPlayerExam(
          resolved.session(), resolved.profile(), latest,
          resolved.event(), resolved.stage(), resolved.workType(), resolved.level()),
          latest, now);
    }
    throw new BusinessException(ErrorCode.CONFLICT, "交卷失败，请稍后重试");
  }

  private CompetitionFormalExamPlayerAnswerSaveVO saveAnswerInCurrentTransaction(
      String sessionIdValue, String questionSnapshotIdValue, CompetitionFormalExamPlayerAnswerSaveReq request) {
    CompetitionExamPrincipal principal = currentPlayer();
    Long sessionId = parseSessionId(sessionIdValue);
    Long questionSnapshotId = parseQuestionSnapshotId(questionSnapshotIdValue);
    if (request == null || request.version() == null || request.version() < 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "答案版本无效");
    }
    ResolvedPlayerExam resolved = resolve(principal, sessionId, true);
    assertCanSaveAnswer(resolved);
    LocalDateTime now = LocalDateTime.now(clock);
    assertBeforeDeadline(resolved.session(), now);

    CompetitionFormalCandidateExamEntity candidate = resolved.candidateExam();
    CompetitionFormalExamQuestionSnapshotEntity question =
        questionSnapshotMapper.selectById(questionSnapshotId);
    if (!belongsToCandidatePaper(resolved.session(), candidate, question)) {
      log.warn("Formal exam cross-paper answer rejected, playerId={}, questionSnapshotId={}",
          principal.getPlayerId(), questionSnapshotId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "题目不属于当前试卷");
    }
    CompetitionFormalCandidateAnswerEntity current =
        answerMapper.selectByCandidateQuestion(candidate.getId(), question.getId());
    AnswerMutation mutation = evaluateAnswer(resolved, question, current, request, now, principal.getPlayerId());
    if (mutation.insert() != null) {
      requireChanged(answerMapper.insert(mutation.insert()), "保存答案失败");
      int answeredCount = refreshAnsweredCount(candidate.getId(), now, principal.getPlayerId());
      return withAnsweredCount(mutation.vo(), answeredCount);
    }
    if (mutation.update() != null) {
      requireChanged(answerMapper.updateById(mutation.update()), "保存答案失败");
      int answeredCount = refreshAnsweredCount(candidate.getId(), now, principal.getPlayerId());
      return withAnsweredCount(mutation.vo(), answeredCount);
    }
    return mutation.vo();
  }

  private AnswerMutation evaluateAnswer(
      ResolvedPlayerExam resolved,
      CompetitionFormalExamQuestionSnapshotEntity question,
      CompetitionFormalCandidateAnswerEntity current,
      CompetitionFormalExamPlayerAnswerSaveReq request,
      LocalDateTime now,
      Long playerId) {
    List<ProjectedOption> options = questionProjection.projectOptions(
        question.getId(), question.getOptionsJson());
    NormalizedAnswer incoming = questionProjection.normalizeAnswer(
        question.getQuestionType(), request.answer(), options);
    boolean marked = Boolean.TRUE.equals(request.marked());
    CompetitionFormalCandidateExamEntity candidate = resolved.candidateExam();
    if (current == null) {
      CompetitionFormalCandidateAnswerEntity created = newAnswer(
          resolved, question, incoming, marked, request.version(), now, playerId);
      return new AnswerMutation(
          toSaveVo(created, incoming.node(), answeredCountOf(candidate), now, true, null),
          created, null);
    }

    int serverVersion = current.getAnswerVersion() == null ? 0 : current.getAnswerVersion();
    NormalizedAnswer stored = questionProjection.parseStored(
        question.getQuestionType(), current.getAnswerJson(), options);
    boolean storedMarked = current.getIsMarked() != null && current.getIsMarked() == 1;
    if (request.version() < serverVersion) {
      return new AnswerMutation(
          toSaveVo(current, stored.node(), answeredCountOf(candidate), now, false, "STALE_VERSION"),
          null, null);
    }
    if (request.version() == serverVersion) {
      if (questionProjection.samePayload(stored, incoming, storedMarked, marked)) {
        return new AnswerMutation(
            toSaveVo(current, stored.node(), answeredCountOf(candidate), now, true, null),
            null, null);
      }
      return new AnswerMutation(
          toSaveVo(current, stored.node(), answeredCountOf(candidate), now, false, "VERSION_PAYLOAD_CONFLICT"),
          null, null);
    }

    if (incoming.answered() && current.getFirstAnswerTime() == null) {
      current.setFirstAnswerTime(now);
    }
    current.setAnswerJson(incoming.json());
    current.setIsAnswered(incoming.answered() ? 1 : 0);
    current.setIsMarked(marked ? 1 : 0);
    current.setAnswerVersion(request.version());
    current.setLastAnswerTime(now);
    current.setUpdatedAt(now);
    current.setUpdatedBy(playerId);
    return new AnswerMutation(
        toSaveVo(current, incoming.node(), answeredCountOf(candidate), now, true, null),
        null, current);
  }

  private Long resolveLoginSession(CompetitionPlayerIdentityEntity player, Long requestedSessionId) {
    if (requestedSessionId != null) {
      List<CompetitionFormalCandidateExamEntity> byPlayer = safe(
          candidateExamMapper.selectBySessionAndPlayerId(requestedSessionId, player.getId()));
      if (byPlayer.size() > 1) {
        log.error("Formal exam login matched multiple CandidateExam rows, playerId={}, sessionId={}",
            player.getId(), requestedSessionId);
        throw new BusinessException(ErrorCode.INTERNAL_ERROR,
            "当前账号在本场比赛存在多份参赛档案，请联系工作人员");
      }
      if (!byPlayer.isEmpty()) {
        CompetitionFormalExamSessionEntity requested = sessionMapper.selectById(requestedSessionId);
        if (requested != null && Objects.equals(requested.getTenantId(), player.getTenantId())) {
          return requestedSessionId;
        }
      }
    }
    Long joinableSessionId = uniqueJoinableSessionId(player.getId());
    if (joinableSessionId != null) {
      if (!Objects.equals(joinableSessionId, requestedSessionId)) {
        log.warn(
            "Formal exam login remapped to the player's current joinable session, playerId={}, requestedSessionId={}, sessionId={}",
            player.getId(), requestedSessionId, joinableSessionId);
      }
      return joinableSessionId;
    }
    if (requestedSessionId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "正式比赛场次不能为空");
    }
    CompetitionFormalExamSessionEntity requested = sessionMapper.selectById(requestedSessionId);
    if (requested == null || !Objects.equals(requested.getTenantId(), player.getTenantId())) {
      log.warn("Formal exam login rejected: session missing or tenant mismatch, playerId={}, sessionId={}",
          player.getId(), requestedSessionId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未参加本场理论考试");
  }

  private Long uniqueJoinableSessionId(Long playerId) {
    LinkedHashSet<Long> sessionIds = new LinkedHashSet<>();
    for (CompetitionFormalCandidateExamEntity row : safe(
        candidateExamMapper.selectJoinableByPlayerId(playerId))) {
      if (row.getExamSessionId() != null) {
        sessionIds.add(row.getExamSessionId());
      }
    }
    if (sessionIds.size() > 1) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "当前账号存在多场可参加的理论考试，请扫描本场二维码");
    }
    return sessionIds.size() == 1 ? sessionIds.getFirst() : null;
  }

  private CompetitionPlayerIdentityEntity authenticateActivePlayer(
      CompetitionFormalExamPlayerLoginReq request) {
    String mobile = playerIdentityService.normalizeMobile(request.account());
    String lookupHash = dataHash.lookupHash(mobile);
    List<CompetitionPlayerIdentityEntity> players =
        playerMapper.selectForUpdateByMobileLookupHashGlobally(lookupHash);
    if (players == null || players.isEmpty()) {
      log.warn("Formal exam login rejected: account not found, lookupHash={}", lookupHash);
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    if (players.size() != 1) {
      log.error("Formal exam login rejected: duplicate player identity, lookupHash={}", lookupHash);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "竞赛账号数据异常");
    }
    CompetitionPlayerIdentityEntity player = players.getFirst();
    if (!CompetitionPlayerStatus.allowsExamAccess(player.getStatus())
        || player.getPasswordHash() == null
        || !passwordEncoder.matches(request.password(), player.getPasswordHash())) {
      log.warn("Formal exam login rejected: invalid credential or blocked player, playerId={}, status={}",
          player.getId(), player.getStatus());
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    return player;
  }

  private ResolvedPlayerExam resolve(
      CompetitionExamPrincipal principal, Long sessionId, boolean lockCandidate) {
    assertAccessSessionScope(principal, sessionId);
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    if (!Objects.equals(principal.getTenantId(), session.getTenantId())) {
      log.warn("Formal exam player rejected: tenant mismatch, playerId={}, sessionId={}",
          principal.getPlayerId(), sessionId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    List<CompetitionContestantProfileEntity> profiles = safe(
        profileMapper.selectByPlayerAndFormalSessionScope(
            principal.getTenantId(), principal.getPlayerId(), session.getEventId(),
            session.getStageId(), session.getEventWorkTypeId(), session.getEventWorkTypeLevelId()));
    if (profiles.isEmpty()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未参加本场理论考试");
    }
    List<Long> profileIds = profiles.stream().map(CompetitionContestantProfileEntity::getId).toList();
    List<CompetitionFormalCandidateExamEntity> candidateExams = safe(
        candidateExamMapper.selectBySessionAndProfileIds(sessionId, profileIds));
    if (candidateExams.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "未找到当前选手的正式考试实例，请联系工作人员");
    }
    if (candidateExams.size() > 1) {
      log.error("Formal exam player matched multiple CandidateExam rows, playerId={}, sessionId={}",
          principal.getPlayerId(), sessionId);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR,
          "当前账号在本场比赛存在多份参赛档案，请联系工作人员");
    }
    CompetitionFormalCandidateExamEntity candidateExam = candidateExams.getFirst();
    if (!Objects.equals(sessionId, candidateExam.getExamSessionId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前考试不属于本场比赛");
    }
    Long contestantProfileId = candidateExam.getContestantProfileId();
    CompetitionContestantProfileEntity profile = profiles.stream()
        .filter(item -> Objects.equals(item.getId(), contestantProfileId))
        .findFirst()
        .orElse(null);
    if (profile == null || !Objects.equals(contestantProfileId, profile.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未参加本场理论考试");
    }
    if (candidateExam.getPlayerId() != null
        && !Objects.equals(candidateExam.getPlayerId(), principal.getPlayerId())) {
      log.warn("Formal exam player rejected: CandidateExam player mismatch, playerId={}, sessionId={}",
          principal.getPlayerId(), sessionId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未参加本场理论考试");
    }
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), session.getEventId());
    CompetitionStageEntity stage = stageMapper.selectById(session.getStageId());
    CompetitionEventWorkTypeEntity workType = workTypeMapper.selectById(session.getEventWorkTypeId());
    CompetitionEventWorkTypeLevelEntity level = levelMapper.selectById(session.getEventWorkTypeLevelId());
    if (lockCandidate) {
      // candidate_exam 行锁是该考生答案写入的唯一串行化锁。
      // 不要再对不存在的 candidate_answer 行使用 FOR UPDATE，
      // 否则 MySQL REPEATABLE READ 会产生 gap lock。
      CompetitionFormalCandidateExamEntity locked =
          candidateExamMapper.selectByIdForUpdate(candidateExam.getId());
      if (locked == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "未找到当前选手的正式考试实例，请联系工作人员");
      }
      if (!Objects.equals(sessionId, locked.getExamSessionId())
          || !Objects.equals(locked.getContestantProfileId(), profile.getId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前考试不属于本场比赛");
      }
      candidateExam = locked;
    }
    return new ResolvedPlayerExam(session, profile, candidateExam, event, stage, workType, level);
  }

  private void assertSessionAllowsFirstEnter(CompetitionFormalExamSessionEntity session) {
    CompetitionFormalExamSessionStatus status =
        CompetitionFormalExamSessionStatus.fromCode(session.getStatus());
    if (status == CompetitionFormalExamSessionStatus.PREPARING) {
      throw new BusinessException(ErrorCode.CONFLICT, "正式比赛数据正在准备，请稍候");
    }
    if (status == CompetitionFormalExamSessionStatus.FINISHED) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场比赛已经结束");
    }
    if (status == CompetitionFormalExamSessionStatus.CANCELLED) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场比赛已经取消");
    }
    if (status == CompetitionFormalExamSessionStatus.RUNNING) {
      throw new BusinessException(ErrorCode.CONFLICT, "比赛已经开始，请联系现场工作人员");
    }
    if (status != CompetitionFormalExamSessionStatus.READY) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前比赛状态不允许进入候考");
    }
  }

  private CompetitionFormalExamPlayerContextVO toContext(ResolvedPlayerExam resolved) {
    CompetitionFormalExamSessionEntity session = resolved.session();
    CompetitionFormalCandidateExamEntity candidate = resolved.candidateExam();
    String runtimeIssue = null;
    if (CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(session.getStatus())
        && CompetitionFormalCandidateExamStatus.READY.getCode().equals(candidate.getStatus())) {
      log.error(
          "Formal exam runtime anomaly: session running but candidate still ready, sessionId={}, candidateExamId={}",
          session.getId(), candidate.getId());
      runtimeIssue = "运行数据异常：比赛已开始但选手仍处于候考状态";
    }
    return new CompetitionFormalExamPlayerContextVO(
        new CompetitionFormalExamPlayerSessionVO(
            id(session.getId()), session.getStatus(), session.getExamType(),
            session.getDurationSeconds(), session.getActualStartTime(), deadlineTime(session)),
        new CompetitionFormalExamPlayerCandidateVO(
            id(candidate.getId()), candidate.getStatus(),
            id(candidate.getContestantProfileId()),
            candidate.getCandidateNo(), candidate.getCandidateName(),
            candidate.getSubmitTime()),
        new CompetitionFormalExamPlayerCompetitionVO(
            resolved.event() == null ? null : resolved.event().getName(),
            resolved.stage() == null ? null : resolved.stage().getName(),
            workTypeName(resolved.workType()),
            resolved.level() == null ? null : resolved.level().getLevelName()),
        LocalDateTime.now(clock),
        runtimeIssue);
  }

  private static void assertAccessSessionScope(CompetitionExamPrincipal principal, Long sessionId) {
    if (principal.getExamSessionId() == null) {
      return;
    }
    if (!Objects.equals(principal.getExamSessionId(), sessionId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前登录不属于本场考试");
    }
  }

  private static LocalDateTime deadlineTime(CompetitionFormalExamSessionEntity session) {
    if (session == null || session.getActualStartTime() == null
        || session.getDurationSeconds() == null || session.getDurationSeconds() <= 0) {
      return null;
    }
    return session.getActualStartTime().plusSeconds(session.getDurationSeconds());
  }

  private void publishCandidateStatus(
      CompetitionExamPrincipal principal,
      Long examSessionId,
      Long candidateExamId,
      String previousStatus,
      String status) {
    if (realtimePublisher == null || principal == null) {
      return;
    }
    realtimePublisher.candidateStatus(
        principal.getTenantId(),
        examSessionId,
        candidateExamId,
        principal.getAccessSessionId(),
        principal.getGeneration(),
        previousStatus,
        status);
  }

  private CompetitionExamPrincipal currentPlayer() {
    try {
      return CompetitionExamSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Formal exam player API called without exam login");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "考试会话无效或已下线");
    }
  }

  private void assertCanLoadPaper(ResolvedPlayerExam resolved) {
    CompetitionFormalExamSessionStatus sessionStatus =
        CompetitionFormalExamSessionStatus.fromCode(resolved.session().getStatus());
    CompetitionFormalCandidateExamStatus candidateStatus =
        CompetitionFormalCandidateExamStatus.fromCode(resolved.candidateExam().getStatus());
    if (candidateStatus == CompetitionFormalCandidateExamStatus.ABSENT) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场考试已记录为缺考");
    }
    if (isSubmitted(candidateStatus)) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场考试已经交卷");
    }
    if (sessionStatus == CompetitionFormalExamSessionStatus.FINISHED) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场比赛已经结束");
    }
    if (sessionStatus == CompetitionFormalExamSessionStatus.CANCELLED) {
      throw new BusinessException(ErrorCode.CONFLICT, "本场比赛已经取消");
    }
    if (sessionStatus == CompetitionFormalExamSessionStatus.PREPARING) {
      throw new BusinessException(ErrorCode.CONFLICT, "正式比赛数据正在准备，请稍候");
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.WAITING_ENTER) {
      throw new BusinessException(ErrorCode.CONFLICT,
          sessionStatus == CompetitionFormalExamSessionStatus.RUNNING
              ? "比赛已经开始，请联系现场工作人员"
              : "请先完成候考进入");
    }
    if (candidateStatus == CompetitionFormalCandidateExamStatus.READY
        || sessionStatus != CompetitionFormalExamSessionStatus.RUNNING) {
      throw new BusinessException(ErrorCode.CONFLICT, "比赛尚未开始");
    }
    if (candidateStatus != CompetitionFormalCandidateExamStatus.RUNNING) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前考试状态不允许作答");
    }
  }

  private void assertCanSaveAnswer(ResolvedPlayerExam resolved) {
    assertCanLoadPaper(resolved);
  }

  private List<CompetitionFormalExamQuestionSnapshotEntity> paperQuestions(ResolvedPlayerExam resolved) {
    CompetitionFormalCandidateExamEntity candidate = resolved.candidateExam();
    if (candidate.getPaperSnapshotId() == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "未找到当前选手的正式试卷");
    }
    List<CompetitionFormalExamQuestionSnapshotEntity> questions = safe(
        questionSnapshotMapper.selectByPaperSnapshotId(candidate.getPaperSnapshotId()));
    if (questions.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "正式试卷题目缺失");
    }
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      if (!belongsToCandidatePaper(resolved.session(), candidate, question)) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "正式试卷题目不属于当前选手");
      }
    }
    return questions;
  }

  private Map<Long, Integer> clientAnswerVersions(
      CompetitionFormalExamPlayerSubmitReq request,
      List<CompetitionFormalExamQuestionSnapshotEntity> questions) {
    List<CompetitionFormalExamPlayerAnswerVersionItem> items =
        request == null || request.answerVersions() == null ? List.of() : request.answerVersions();
    if (items.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "交卷答案版本清单不能为空");
    }
    Map<Long, Integer> clientVersions = new LinkedHashMap<>();
    for (CompetitionFormalExamPlayerAnswerVersionItem item : items) {
      if (item == null || item.version() == null || item.version() < 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "答案版本无效");
      }
      Long questionSnapshotId = parseQuestionSnapshotId(item.questionSnapshotId());
      if (clientVersions.containsKey(questionSnapshotId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "交卷答案版本清单存在重复题目");
      }
      clientVersions.put(questionSnapshotId, item.version());
    }
    Set<Long> paperIds = new LinkedHashSet<>();
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      paperIds.add(question.getId());
    }
    if (!paperIds.equals(clientVersions.keySet())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "交卷答案版本清单与当前试卷不一致");
    }
    return clientVersions;
  }

  private Map<Long, Integer> serverAnswerVersions(
      Long candidateExamId, List<CompetitionFormalExamQuestionSnapshotEntity> questions) {
    Map<Long, Integer> versions = new LinkedHashMap<>();
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      versions.put(question.getId(), 0);
    }
    for (CompetitionFormalCandidateAnswerEntity answer : safe(answerMapper.selectByCandidateExamId(candidateExamId))) {
      if (answer.getQuestionSnapshotId() == null || !versions.containsKey(answer.getQuestionSnapshotId())) {
        continue;
      }
      versions.put(answer.getQuestionSnapshotId(), answer.getAnswerVersion() == null ? 0 : answer.getAnswerVersion());
    }
    return versions;
  }

  private CompetitionFormalExamPlayerSubmitVO toSubmitVo(
      ResolvedPlayerExam resolved, CompetitionFormalCandidateExamEntity candidate, LocalDateTime now) {
    List<CompetitionFormalExamQuestionSnapshotEntity> questions = paperQuestions(resolved);
    int answeredCount = answerMapper.countAnswered(candidate.getId());
    int totalQuestionCount = questions.size();
    int unansweredCount = Math.max(0, totalQuestionCount - answeredCount);
    return toSubmitVo(candidate, now, answeredCount, totalQuestionCount, unansweredCount);
  }

  private CompetitionFormalExamPlayerSubmitVO toSubmitVo(
      CompetitionFormalCandidateExamEntity candidate,
      LocalDateTime now,
      int answeredCount,
      int totalQuestionCount,
      int unansweredCount) {
    return new CompetitionFormalExamPlayerSubmitVO(
        candidate.getStatus(),
        candidate.getSubmitTime(),
        answeredCount,
        totalQuestionCount,
        unansweredCount,
        now);
  }

  private void assertBeforeDeadline(CompetitionFormalExamSessionEntity session, LocalDateTime now) {
    LocalDateTime deadline = deadlineTime(session);
    if (deadline == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "正式比赛缺少截止时间，不能保存答案");
    }
    if (!now.isBefore(deadline)) {
      throw new BusinessException(ErrorCode.CONFLICT, "考试时间已到，请等待系统处理");
    }
  }

  private CompetitionFormalExamPlayerPaperVO toPaper(ResolvedPlayerExam resolved, LocalDateTime now) {
    CompetitionFormalCandidateExamEntity candidate = resolved.candidateExam();
    CompetitionFormalExamPaperSnapshotEntity paper =
        paperSnapshotMapper.selectById(candidate.getPaperSnapshotId());
    if (paper == null
        || !Objects.equals(paper.getExamSessionId(), resolved.session().getId())
        || !Objects.equals(paper.getId(), candidate.getPaperSnapshotId())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "未找到当前选手的正式试卷");
    }
    List<CompetitionFormalExamQuestionSnapshotEntity> questions = safe(
        questionSnapshotMapper.selectByPaperSnapshotId(paper.getId()));
    if (questions.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "正式试卷题目缺失");
    }
    Map<Long, CompetitionFormalCandidateAnswerEntity> answersByQuestion = new LinkedHashMap<>();
    for (CompetitionFormalCandidateAnswerEntity answer : safe(
        answerMapper.selectByCandidateExamId(candidate.getId()))) {
      answersByQuestion.put(answer.getQuestionSnapshotId(), answer);
    }
    List<CompetitionFormalExamPlayerQuestionVO> questionVos = new ArrayList<>();
    List<CompetitionFormalExamPlayerSavedAnswerVO> answerVos = new ArrayList<>();
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      List<ProjectedOption> options = questionProjection.projectOptions(
          question.getId(), question.getOptionsJson());
      questionVos.add(new CompetitionFormalExamPlayerQuestionVO(
          id(question.getId()),
          question.getQuestionOrder(),
          question.getQuestionType(),
          question.getStem(),
          question.getScore(),
          options.stream()
              .map(option -> new CompetitionFormalExamPlayerOptionVO(
                  option.optionId(), option.label(), option.content()))
              .toList()));
      CompetitionFormalCandidateAnswerEntity saved = answersByQuestion.get(question.getId());
      if (saved == null) {
        continue;
      }
      NormalizedAnswer stored = questionProjection.parseStored(
          question.getQuestionType(), saved.getAnswerJson(), options);
      answerVos.add(new CompetitionFormalExamPlayerSavedAnswerVO(
          id(question.getId()),
          stored.node(),
          saved.getIsMarked() != null && saved.getIsMarked() == 1,
          saved.getAnswerVersion() == null ? 0 : saved.getAnswerVersion(),
          saved.getIsAnswered() != null && saved.getIsAnswered() == 1,
          saved.getLastAnswerTime()));
    }
    return new CompetitionFormalExamPlayerPaperVO(
        new CompetitionFormalExamPlayerPaperSessionVO(
            id(resolved.session().getId()), resolved.session().getStatus(), now,
            resolved.session().getActualStartTime(), deadlineTime(resolved.session()),
            resolved.session().getDurationSeconds()),
        new CompetitionFormalExamPlayerPaperCandidateVO(
            candidate.getCandidateNo(), candidate.getCandidateName()),
        new CompetitionFormalExamPlayerPaperMetaVO(
            questions.size(), paper.getTotalScore(), answeredCountOf(candidate)),
        questionVos,
        answerVos);
  }

  private boolean belongsToCandidatePaper(
      CompetitionFormalExamSessionEntity session,
      CompetitionFormalCandidateExamEntity candidate,
      CompetitionFormalExamQuestionSnapshotEntity question) {
    return question != null
        && Objects.equals(question.getExamSessionId(), session.getId())
        && Objects.equals(question.getPaperSnapshotId(), candidate.getPaperSnapshotId())
        && Objects.equals(question.getTenantId(), session.getTenantId());
  }

  private CompetitionFormalCandidateAnswerEntity newAnswer(
      ResolvedPlayerExam resolved,
      CompetitionFormalExamQuestionSnapshotEntity question,
      NormalizedAnswer incoming,
      boolean marked,
      int version,
      LocalDateTime now,
      Long playerId) {
    CompetitionFormalCandidateAnswerEntity created = new CompetitionFormalCandidateAnswerEntity();
    created.setId(IdWorker.getId());
    created.setTenantId(resolved.session().getTenantId());
    created.setExamSessionId(resolved.session().getId());
    created.setCandidateExamId(resolved.candidateExam().getId());
    created.setQuestionSnapshotId(question.getId());
    created.setAnswerJson(incoming.json());
    created.setIsAnswered(incoming.answered() ? 1 : 0);
    created.setIsMarked(marked ? 1 : 0);
    created.setAnswerVersion(version);
    created.setFirstAnswerTime(incoming.answered() ? now : null);
    created.setLastAnswerTime(now);
    created.setCreatedBy(playerId);
    created.setUpdatedBy(playerId);
    created.setCreatedAt(now);
    created.setUpdatedAt(now);
    created.setDeleted(0);
    return created;
  }

  private int refreshAnsweredCount(Long candidateExamId, LocalDateTime now, Long playerId) {
    int answeredCount = answerMapper.countAnswered(candidateExamId);
    requireChanged(
        candidateExamMapper.updateAnsweredCount(candidateExamId, answeredCount, now, playerId),
        "更新已答数量失败");
    return answeredCount;
  }

  private static int answeredCountOf(CompetitionFormalCandidateExamEntity candidate) {
    return candidate.getAnsweredCount() == null ? 0 : candidate.getAnsweredCount();
  }

  private CompetitionFormalExamPlayerAnswerSaveVO toSaveVo(
      CompetitionFormalCandidateAnswerEntity answer,
      com.fasterxml.jackson.databind.JsonNode payload,
      int answeredCount,
      LocalDateTime now,
      boolean accepted,
      String conflictCode) {
    return new CompetitionFormalExamPlayerAnswerSaveVO(
        id(answer.getQuestionSnapshotId()),
        answer.getAnswerVersion(),
        answer.getIsAnswered() != null && answer.getIsAnswered() == 1,
        answer.getIsMarked() != null && answer.getIsMarked() == 1,
        answer.getLastAnswerTime(),
        answeredCount,
        now,
        accepted,
        conflictCode,
        payload,
        saveStatus(accepted, conflictCode));
  }

  private CompetitionFormalExamPlayerAnswerSaveVO rejectedSave(
      CompetitionFormalExamPlayerAnswerSyncItemReq item, BusinessException ex) {
    String conflictCode = conflictCodeOf(ex);
    int version = item.version() == null ? 0 : item.version();
    return new CompetitionFormalExamPlayerAnswerSaveVO(
        item.questionSnapshotId(),
        version,
        false,
        Boolean.TRUE.equals(item.marked()),
        null,
        0,
        LocalDateTime.now(clock),
        false,
        conflictCode,
        item.answer(),
        conflictCode);
  }

  private static String saveStatus(boolean accepted, String conflictCode) {
    if (accepted) {
      return "SYNCED";
    }
    return StringUtils.hasText(conflictCode) ? conflictCode : "REJECTED";
  }

  private static String conflictCodeOf(BusinessException ex) {
    if (ex.getMessage() != null && ex.getMessage().contains("考试时间已到")) {
      return "EXAM_TIME_EXPIRED";
    }
    if (ErrorCode.FORBIDDEN.equals(ex.getCode())) {
      return "FORBIDDEN";
    }
    if (ErrorCode.CONFLICT.equals(ex.getCode())) {
      return "CONFLICT";
    }
    if (ErrorCode.BAD_REQUEST.equals(ex.getCode()) || ErrorCode.UNPROCESSABLE_ENTITY.equals(ex.getCode())) {
      return "REJECTED";
    }
    return StringUtils.hasText(ex.getCode()) ? ex.getCode() : "REJECTED";
  }

  private static void requireChanged(int changed, String message) {
    if (changed != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, message);
    }
  }

  private static Long parseQuestionSnapshotId(String value) {
    if (!StringUtils.hasText(value)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题目标识不能为空");
    }
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题目标识无效");
    }
  }

  private static Long parseOptionalSessionId(String sessionIdValue) {
    if (!StringUtils.hasText(sessionIdValue)) {
      return null;
    }
    try {
      return Long.valueOf(sessionIdValue.trim());
    } catch (NumberFormatException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "正式比赛场次无效");
    }
  }

  private static Long parseSessionId(String sessionIdValue) {
    Long sessionId = parseOptionalSessionId(sessionIdValue);
    if (sessionId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "正式比赛场次不能为空");
    }
    return sessionId;
  }

  private static boolean isSubmitted(CompetitionFormalCandidateExamStatus status) {
    return status != null && status.isTerminal();
  }

  private static String workTypeName(CompetitionEventWorkTypeEntity workType) {
    if (workType == null) {
      return null;
    }
    return StringUtils.hasText(workType.getDisplayName())
        ? workType.getDisplayName() : String.valueOf(workType.getWorkTypeId());
  }

  private static String id(Long value) {
    return value == null ? null : String.valueOf(value);
  }

  private static <T> List<T> safe(List<T> values) {
    return values == null ? new ArrayList<>() : values.stream()
        .filter(Objects::nonNull)
        .toList();
  }

  private ResolvedStatus resolveStatus(CompetitionExamPrincipal principal, Long sessionId) {
    assertAccessSessionScope(principal, sessionId);
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    if (!Objects.equals(principal.getTenantId(), session.getTenantId())) {
      log.warn("Formal exam player rejected: tenant mismatch, playerId={}, sessionId={}",
          principal.getPlayerId(), sessionId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    List<CompetitionFormalCandidateExamEntity> byPlayer = safe(
        candidateExamMapper.selectBySessionAndPlayerId(sessionId, principal.getPlayerId()));
    if (byPlayer.size() > 1) {
      log.error("Formal exam player matched multiple CandidateExam rows, playerId={}, sessionId={}",
          principal.getPlayerId(), sessionId);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR,
          "当前账号在本场比赛存在多份参赛档案，请联系工作人员");
    }
    if (byPlayer.size() == 1) {
      return new ResolvedStatus(session, byPlayer.getFirst());
    }
    ResolvedPlayerExam fallback = resolve(principal, sessionId, false);
    return new ResolvedStatus(fallback.session(), fallback.candidateExam());
  }

  private CompetitionFormalExamPlayerStatusVO toStatus(ResolvedStatus resolved) {
    CompetitionFormalExamSessionEntity session = resolved.session();
    CompetitionFormalCandidateExamEntity candidate = resolved.candidateExam();
    String runtimeIssue = null;
    if (CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(session.getStatus())
        && CompetitionFormalCandidateExamStatus.READY.getCode().equals(candidate.getStatus())) {
      log.error(
          "Formal exam runtime anomaly: session running but candidate still ready, sessionId={}, candidateExamId={}",
          session.getId(), candidate.getId());
      runtimeIssue = "运行数据异常：比赛已开始但选手仍处于候考状态";
    }
    return new CompetitionFormalExamPlayerStatusVO(
        new CompetitionFormalExamPlayerSessionVO(
            id(session.getId()), session.getStatus(), session.getExamType(),
            session.getDurationSeconds(), session.getActualStartTime(), deadlineTime(session)),
        new CompetitionFormalExamPlayerStatusCandidateVO(
            id(candidate.getId()), candidate.getStatus(),
            answeredCountOf(candidate),
            candidate.getTotalQuestionCount(),
            candidate.getSubmitTime(),
            candidate.getLastActiveTime()),
        LocalDateTime.now(clock),
        runtimeIssue);
  }

  private static boolean isFirstOccurrence(List<BatchSyncItem> work, BatchSyncItem current) {
    for (BatchSyncItem item : work) {
      if (item.questionId() != null && item.questionId().equals(current.questionId())) {
        return item == current;
      }
    }
    return true;
  }

  private static CompetitionFormalExamPlayerAnswerSaveVO withAnsweredCount(
      CompetitionFormalExamPlayerAnswerSaveVO vo, int answeredCount) {
    return new CompetitionFormalExamPlayerAnswerSaveVO(
        vo.questionSnapshotId(), vo.version(), vo.isAnswered(), vo.marked(),
        vo.savedAt(), answeredCount, vo.serverTime(), vo.accepted(),
        vo.conflictCode(), vo.answer(), vo.status());
  }

  private record BatchSyncItem(
      CompetitionFormalExamPlayerAnswerSyncItemReq item,
      Long questionId,
      BusinessException error) {
  }

  private record AnswerMutation(
      CompetitionFormalExamPlayerAnswerSaveVO vo,
      CompetitionFormalCandidateAnswerEntity insert,
      CompetitionFormalCandidateAnswerEntity update) {
  }

  private record ResolvedStatus(
      CompetitionFormalExamSessionEntity session,
      CompetitionFormalCandidateExamEntity candidateExam) {
  }

  private record ResolvedPlayerExam(
      CompetitionFormalExamSessionEntity session,
      CompetitionContestantProfileEntity profile,
      CompetitionFormalCandidateExamEntity candidateExam,
      CompetitionEventEntity event,
      CompetitionStageEntity stage,
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level) {
  }
}
