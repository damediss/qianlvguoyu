package com.guoyu.platform.competition.exam.service;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAnswerSaveRequestEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionTokenAuditEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
import com.guoyu.platform.competition.exam.dto.CompetitionExamAnswerSaveReq;
import com.guoyu.platform.competition.exam.dto.CompetitionExamLoginReq;
import com.guoyu.platform.competition.exam.security.CompetitionExamPrincipal;
import com.guoyu.platform.competition.exam.security.CompetitionExamSecurityContext;
import com.guoyu.platform.competition.exam.vo.CompetitionExamAnswerVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamAttemptVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamLoginVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamQuestionVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamSubmissionVO;
import com.guoyu.platform.competition.exam.vo.CompetitionExamWaitingVO;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAnswerSaveRequestMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamAssignmentRow;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionTokenAuditMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import com.guoyu.platform.competition.service.CompetitionContestantScopeValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
public class CompetitionExamApplicationService {
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

  private final CompetitionPlayerIdentityMapper playerMapper;
  private final CompetitionPlayerIdentityDomainService playerIdentityService;
  private final CompetitionPlayerDataHash dataHash;
  private final PasswordEncoder passwordEncoder;
  private final CompetitionTheoryCandidateAssignmentMapper assignmentMapper;
  private final CompetitionTheoryAttemptMapper attemptMapper;
  private final CompetitionTheoryExamSessionMapper sessionMapper;
  private final CompetitionTheoryPaperQuestionMapper questionMapper;
  private final CompetitionTheoryAnswerMapper answerMapper;
  private final CompetitionTheoryAnswerSaveRequestMapper saveRequestMapper;
  private final CompetitionTheoryExamSessionTokenAuditMapper auditMapper;
  private final CompetitionTheorySubmissionService submissionService;
  private final StpLogic examStpLogic;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final CompetitionContestantScopeValidator contestantScopeValidator;

  public CompetitionExamApplicationService(
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionPlayerIdentityDomainService playerIdentityService,
      CompetitionPlayerDataHash dataHash,
      PasswordEncoder passwordEncoder,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionTheoryPaperQuestionMapper questionMapper,
      CompetitionTheoryAnswerMapper answerMapper,
      CompetitionTheoryAnswerSaveRequestMapper saveRequestMapper,
      CompetitionTheoryExamSessionTokenAuditMapper auditMapper,
      CompetitionTheorySubmissionService submissionService,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      ObjectMapper objectMapper,
      Clock clock) {
    this(playerMapper, playerIdentityService, dataHash, passwordEncoder, assignmentMapper, attemptMapper,
        sessionMapper, questionMapper, answerMapper, saveRequestMapper, auditMapper, submissionService,
        examStpLogic, objectMapper, clock, null);
  }

  @Autowired
  public CompetitionExamApplicationService(
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionPlayerIdentityDomainService playerIdentityService,
      CompetitionPlayerDataHash dataHash,
      PasswordEncoder passwordEncoder,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionTheoryPaperQuestionMapper questionMapper,
      CompetitionTheoryAnswerMapper answerMapper,
      CompetitionTheoryAnswerSaveRequestMapper saveRequestMapper,
      CompetitionTheoryExamSessionTokenAuditMapper auditMapper,
      CompetitionTheorySubmissionService submissionService,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      ObjectMapper objectMapper,
      Clock clock,
      CompetitionContestantScopeValidator contestantScopeValidator) {
    this.playerMapper = playerMapper;
    this.playerIdentityService = playerIdentityService;
    this.dataHash = dataHash;
    this.passwordEncoder = passwordEncoder;
    this.assignmentMapper = assignmentMapper;
    this.attemptMapper = attemptMapper;
    this.sessionMapper = sessionMapper;
    this.questionMapper = questionMapper;
    this.answerMapper = answerMapper;
    this.saveRequestMapper = saveRequestMapper;
    this.auditMapper = auditMapper;
    this.submissionService = submissionService;
    this.examStpLogic = examStpLogic;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.contestantScopeValidator = contestantScopeValidator;
  }

  @Transactional
  public CompetitionExamLoginVO login(CompetitionExamLoginReq request) {
    String mobile = playerIdentityService.normalizeMobile(request.account());
    String lookupHash = dataHash.lookupHash(mobile);
    List<CompetitionPlayerIdentityEntity> players =
        playerMapper.selectForUpdateByMobileLookupHashGlobally(lookupHash);
    if (players == null || players.isEmpty()) {
      log.warn("Competition exam login rejected: account not found, lookupHash={}", lookupHash);
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    if (players.size() != 1) {
      log.error("Competition exam login rejected: duplicate player identity, lookupHash={}", lookupHash);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "竞赛账号数据异常");
    }
    CompetitionPlayerIdentityEntity player = players.getFirst();
    if (!CompetitionPlayerStatus.ACTIVE.getCode().equals(player.getStatus())
        || player.getPasswordHash() == null
        || !passwordEncoder.matches(request.password(), player.getPasswordHash())) {
      log.warn("Competition exam login rejected: invalid credential or inactive player, playerId={}",
          player.getId());
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    if (examStpLogic.isLogin(player.getId())) {
      log.warn("Competition exam second device login rejected, playerId={}", player.getId());
      throw new BusinessException(ErrorCode.CONFLICT, "该选手已有有效考试会话，请联系监考人员");
    }

    LocalDateTime now = LocalDateTime.now(clock);
    List<CompetitionTheoryExamAssignmentRow> candidates =
        assignmentMapper.selectExamLoginCandidatesGlobally(player.getId(), now);
    if (candidates == null || candidates.isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "当前没有可登录的考试安排");
    }
    if (candidates.size() > 1) {
      log.error("Competition exam login found overlapping assignments, playerId={}", player.getId());
      throw new BusinessException(ErrorCode.CONFLICT, "存在冲突的考试安排，请联系监考人员");
    }
    CompetitionTheoryExamAssignmentRow assignment = candidates.getFirst();
    Long originalTenant = TenantContextHolder.getTenantId();
    TenantContextHolder.setTenantId(assignment.getTenantId());
    try {
      CompetitionTheoryAttemptEntity attempt = ensureAttempt(assignment, now);
      if (now.isAfter(assignment.getStartAt().plusMinutes(30))
          && (attempt.getRestoreCount() == null || attempt.getRestoreCount() == 0)
          && !"in_progress".equals(attempt.getStatus())) {
        attemptMapper.markProvisionalAbsent(attempt.getId(), now);
        throw new BusinessException(ErrorCode.FORBIDDEN, "已超过迟到登录时间，请联系监考人员");
      }
      if ("provisional_absent".equals(attempt.getStatus())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前为暂定缺考，请联系监考人员恢复");
      }
      if (isTerminal(attempt.getStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT, "本场考试已经结束");
      }
      SaLoginParameter loginParameter = SaLoginParameter.create()
          .setIsConcurrent(false)
          .setIsShare(false)
          .setMaxLoginCount(1);
      examStpLogic.login(player.getId(), loginParameter);
      String token = examStpLogic.getTokenValue();
      if (token == null || token.isBlank()) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试会话创建失败");
      }
      attemptMapper.updateSessionState(attempt.getId(), "active", now);
      writeAudit(attempt, "login", "active", null, null, fingerprint(token), now);
      return new CompetitionExamLoginVO(
          player.getId(), assignment.getTenantId(), token, "Bearer",
          examStpLogic.getTokenTimeout(), attempt.getId());
    } finally {
            if (originalTenant == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(originalTenant);
            }
    }
  }

  @Transactional(readOnly = true)
  public CompetitionExamWaitingVO waiting() {
    CompetitionTheoryAttemptEntity attempt = currentAttempt();
    CompetitionTheoryExamAssignmentRow assignment = requireAssignment(attempt);
    LocalDateTime now = LocalDateTime.now(clock);
    boolean canStart = !now.isBefore(assignment.getStartAt())
        && now.isBefore(assignment.getEndAt())
        && !"provisional_absent".equals(attempt.getStatus());
    return new CompetitionExamWaitingVO(
        attempt.getId(), attempt.getStatus(), assignment.getEventName(), assignment.getWorkTypeName(),
        assignment.getSessionName(), assignment.getRoomName(), assignment.getAddress(),
        assignment.getSeatNo(), assignment.getStartAt(), assignment.getEndAt(), now,
        secondsBetween(now, assignment.getStartAt()),
        remaining(now, assignment.getEndAt()), canStart);
  }

  @Transactional
  public CompetitionExamAttemptVO startOrResume() {
    CompetitionTheoryAttemptEntity attempt = currentAttemptForUpdate();
    CompetitionTheoryExamAssignmentRow assignment = requireAssignment(attempt);
    LocalDateTime now = LocalDateTime.now(clock);
    if (now.isBefore(assignment.getStartAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "考试尚未开放");
    }
    if (!now.isBefore(assignment.getEndAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "考试已经结束");
    }
    if ("provisional_absent".equals(attempt.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "暂定缺考需由监考人员恢复");
    }
    if (now.isAfter(assignment.getStartAt().plusMinutes(30))
        && (attempt.getRestoreCount() == null || attempt.getRestoreCount() == 0)
        && !"in_progress".equals(attempt.getStatus())) {
      attemptMapper.markProvisionalAbsent(attempt.getId(), now);
      throw new BusinessException(ErrorCode.FORBIDDEN, "已超过迟到开始时间");
    }
    if (!"in_progress".equals(attempt.getStatus())) {
      sessionMapper.openIfDue(assignment.getSessionId(), now);
      if (attemptMapper.start(attempt.getId(), now) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "考试状态已发生变化");
      }
      writeAudit(attempt, "start", "active", null, null, null, now);
    }
    return attemptView(attempt.getId(), assignment, now);
  }

  @Transactional(readOnly = true)
  public List<CompetitionExamQuestionVO> questions() {
    CompetitionTheoryAttemptEntity attempt = requireInProgressCurrent();
    assertBeforeEnd(requireAssignment(attempt), LocalDateTime.now(clock));
    List<CompetitionTheoryPaperQuestionEntity> questions =
        questionMapper.selectByPaper(attempt.getPaperSnapshotId());
    if (questions == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试卷题目查询失败");
    }
    List<CompetitionExamQuestionVO> result = new ArrayList<>(questions.size());
    for (CompetitionTheoryPaperQuestionEntity question : questions) {
      result.add(new CompetitionExamQuestionVO(
          question.getId(), question.getSortOrder(), question.getQuestionType(),
          question.getContent(), parseTree(question.getOptionsJson()), question.getScore()));
    }
    return result;
  }

  @Transactional
  public CompetitionExamAnswerVO saveAnswer(CompetitionExamAnswerSaveReq request) {
    CompetitionTheoryAttemptEntity attempt = currentAttemptForUpdate();
    if (!"in_progress".equals(attempt.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前考试状态不可保存答案");
    }
    LocalDateTime now = LocalDateTime.now(clock);
    assertBeforeEnd(requireAssignment(attempt), now);
    CompetitionTheoryPaperQuestionEntity question = questionMapper.selectById(request.questionId());
    if (question == null || !attempt.getPaperSnapshotId().equals(question.getPaperSnapshotId())) {
      log.warn("Competition exam cross-paper answer rejected, playerId={}, questionId={}",
          attempt.getPlayerId(), request.questionId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "题目不属于当前试卷");
    }
    List<String> keys = normalizeAndValidateKeys(request.answerKeys(), question.getOptionsJson());
    String answerJson = writeKeys(keys);
    String answerHash = fingerprint(answerJson + "|" + request.markedForReview());
    CompetitionTheoryAnswerSaveRequestEntity replay =
        saveRequestMapper.selectByRequest(attempt.getId(), request.requestId());
    if (replay != null) {
      if (!request.questionId().equals(replay.getQuestionSnapshotId())
          || !answerHash.equals(replay.getAnswerHash())) {
        log.warn("Competition exam answer request id reused with different payload, playerId={}, requestId={}",
            attempt.getPlayerId(), request.requestId());
        throw new BusinessException(ErrorCode.CONFLICT, "答案保存请求标识已被其他内容使用");
      }
      CompetitionTheoryAnswerEntity current = answerMapper.selectForUpdate(
          attempt.getId(), request.questionId());
      return toAnswerVO(current, replay.getAcceptedVersion(), replay.getSavedAt(), keys,
          request.markedForReview());
    }

    CompetitionTheoryAnswerEntity answer = answerMapper.selectForUpdate(
        attempt.getId(), request.questionId());
    int nextVersion = answer == null ? 1 : answer.getSaveVersion() + 1;
    if (answer == null) {
      answer = new CompetitionTheoryAnswerEntity();
      answer.setTenantId(attempt.getTenantId());
      answer.setAttemptId(attempt.getId());
      answer.setPaperSnapshotId(attempt.getPaperSnapshotId());
      answer.setQuestionSnapshotId(question.getId());
      answer.setQuestionScore(question.getScore());
      answer.setScoringRuleCode(question.getScoringRuleCode());
      answer.setScoringRuleVersion(question.getScoringRuleVersion());
      answer.setDeleted(0);
    }
    answer.setAnswerJson(keys.isEmpty() ? null : answerJson);
    answer.setMarkedForReview(request.markedForReview() ? 1 : 0);
    answer.setSaveVersion(nextVersion);
    answer.setLastRequestId(request.requestId());
    answer.setLastSavedAt(now);
    answer.setAwardedScore(null);
    answer.setCorrectFlag(null);
    answer.setScoredAt(null);
    int changed = answer.getId() == null ? answerMapper.insert(answer) : answerMapper.updateById(answer);
    if (changed != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答案保存失败");
    }

    CompetitionTheoryAnswerSaveRequestEntity ledger =
        new CompetitionTheoryAnswerSaveRequestEntity();
    ledger.setTenantId(attempt.getTenantId());
    ledger.setAttemptId(attempt.getId());
    ledger.setQuestionSnapshotId(question.getId());
    ledger.setRequestId(request.requestId());
    ledger.setAcceptedVersion(nextVersion);
    ledger.setAnswerHash(answerHash);
    ledger.setSavedAt(now);
    ledger.setDeleted(0);
    if (saveRequestMapper.insert(ledger) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答案幂等台账保存失败");
    }
    attempt.setLastSavedAt(now);
    attempt.setSessionState("active");
    attempt.setLastSessionEventAt(now);
    if (attemptMapper.updateById(attempt) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试保存时间更新失败");
    }
    return new CompetitionExamAnswerVO(
        question.getId(), keys, request.markedForReview(), nextVersion, now);
  }

  @Transactional(readOnly = true)
  public CompetitionExamAttemptVO current() {
    CompetitionTheoryAttemptEntity attempt = currentAttempt();
    return attemptView(attempt.getId(), requireAssignment(attempt), LocalDateTime.now(clock));
  }

  public CompetitionExamSubmissionVO submit(String requestId) {
    CompetitionTheoryAttemptEntity attempt = currentAttempt();
    CompetitionTheoryAttemptEntity submitted = submissionService.submit(
        attempt.getId(), "manual", requestId, null, null, LocalDateTime.now(clock));
    examStpLogic.logout(attempt.getPlayerId());
    return submissionView(submitted);
  }

  @Transactional(readOnly = true)
  public CompetitionExamSubmissionVO submissionStatus() {
    return submissionView(currentAttempt());
  }

  @Transactional
  public void logout() {
    CompetitionTheoryAttemptEntity attempt = currentAttemptForCleanup();
    LocalDateTime now = LocalDateTime.now(clock);
    writeAudit(attempt, "logout", "closed", null, null, null, now);
    attemptMapper.updateSessionState(attempt.getId(), "offline", now);
    afterCommitTokenAction(() -> examStpLogic.logout(attempt.getPlayerId()));
  }

  private void afterCommitTokenAction(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        action.run();
      }
    });
  }

  private CompetitionTheoryAttemptEntity ensureAttempt(
      CompetitionTheoryExamAssignmentRow assignment, LocalDateTime now) {
    if (contestantScopeValidator != null) {
      contestantScopeValidator.validateForUpdate(
          assignment.getTenantId(), assignment.getEventId(), assignment.getStageId(),
          assignment.getEventWorkTypeId(), assignment.getContestantMemberId());
    }
    CompetitionTheoryAttemptEntity existing =
        attemptMapper.selectForUpdateByAssignment(assignment.getAssignmentId());
    if (existing != null) {
      return existing;
    }
    CompetitionTheoryAttemptEntity attempt = new CompetitionTheoryAttemptEntity();
    attempt.setTenantId(assignment.getTenantId());
    attempt.setAssignmentId(assignment.getAssignmentId());
    attempt.setScheduleVersionId(assignment.getScheduleVersionId());
    attempt.setSessionId(assignment.getSessionId());
    attempt.setPaperSnapshotId(assignment.getPaperSnapshotId());
    attempt.setEventId(assignment.getEventId());
    attempt.setStageId(assignment.getStageId());
    attempt.setEventWorkTypeId(assignment.getEventWorkTypeId());
    attempt.setContestantProfileId(assignment.getContestantProfileId());
    attempt.setContestantMemberId(assignment.getContestantMemberId());
    attempt.setPlayerId(assignment.getPlayerId());
    attempt.setStatus(now.isBefore(assignment.getStartAt()) ? "waiting" : "ready");
    attempt.setSessionState("offline");
    attempt.setAbsenceFlag(0);
    attempt.setRestoreCount(0);
    attempt.setLockVersion(0);
    attempt.setDeleted(0);
    if (attemptMapper.insert(attempt) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试作答记录创建失败");
    }
    return attempt;
  }

  private CompetitionTheoryAttemptEntity currentAttempt() {
    CompetitionExamPrincipal principal = currentPlayer();
    CompetitionTheoryAttemptEntity attempt = attemptMapper.selectCurrentByPlayer(principal.getPlayerId());
    if (attempt == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "当前没有进行中的考试");
    }
    if (!principal.getTenantId().equals(attempt.getTenantId())) {
      log.warn("Competition exam attempt tenant mismatch, playerId={}", principal.getPlayerId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "考试租户范围不匹配");
    }
    return attempt;
  }

  private CompetitionTheoryAttemptEntity currentAttemptForUpdate() {
    CompetitionTheoryAttemptEntity current = currentAttempt();
    if (contestantScopeValidator != null) {
      contestantScopeValidator.validateForUpdate(
          current.getTenantId(), current.getEventId(), current.getStageId(),
          current.getEventWorkTypeId(), current.getContestantMemberId());
    }
    CompetitionTheoryAttemptEntity locked = attemptMapper.selectForUpdateById(current.getId());
    if (locked == null || !currentPlayer().getPlayerId().equals(locked.getPlayerId())) {
      log.warn("Competition exam cross-attempt access rejected");
      throw new BusinessException(ErrorCode.FORBIDDEN, "考试作答范围不匹配");
    }
    return locked;
  }

  private CompetitionTheoryAttemptEntity currentAttemptForCleanup() {
    CompetitionTheoryAttemptEntity current = currentAttempt();
    if (contestantScopeValidator != null) {
      contestantScopeValidator.validateForUpdate(
          current.getTenantId(), current.getEventId(), current.getStageId(),
          current.getEventWorkTypeId(), current.getContestantMemberId(),
          CompetitionContestantScopeValidator.UpdateIntent.CLEANUP_ONLY);
    }
    CompetitionTheoryAttemptEntity locked = attemptMapper.selectForUpdateById(current.getId());
    if (locked == null || !currentPlayer().getPlayerId().equals(locked.getPlayerId())) {
      log.warn("Competition exam cleanup cross-attempt access rejected");
      throw new BusinessException(ErrorCode.FORBIDDEN, "考试作答范围不匹配");
    }
    return locked;
  }

  private CompetitionTheoryAttemptEntity requireInProgressCurrent() {
    CompetitionTheoryAttemptEntity attempt = currentAttempt();
    if (!"in_progress".equals(attempt.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "考试尚未开始或已经结束");
    }
    return attempt;
  }

  private CompetitionTheoryExamAssignmentRow requireAssignment(CompetitionTheoryAttemptEntity attempt) {
    CompetitionTheoryExamAssignmentRow assignment = assignmentMapper.selectExamAssignment(
        attempt.getTenantId(), attempt.getAssignmentId());
    if (assignment == null || !attempt.getPlayerId().equals(assignment.getPlayerId())) {
      log.warn("Competition exam assignment mismatch, attemptId={}", attempt.getId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "考试安排范围不匹配");
    }
    return assignment;
  }

  private CompetitionExamAttemptVO attemptView(
      Long attemptId, CompetitionTheoryExamAssignmentRow assignment, LocalDateTime now) {
    CompetitionTheoryAttemptEntity attempt = attemptMapper.selectById(attemptId);
    if (attempt == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试作答记录不存在");
    }
    List<CompetitionTheoryAnswerEntity> answers = answerMapper.selectByAttempt(attemptId);
    List<CompetitionExamAnswerVO> answerViews = new ArrayList<>();
    if (answers != null) {
      for (CompetitionTheoryAnswerEntity answer : answers) {
        answerViews.add(new CompetitionExamAnswerVO(
            answer.getQuestionSnapshotId(), parseKeys(answer.getAnswerJson()),
            Integer.valueOf(1).equals(answer.getMarkedForReview()), answer.getSaveVersion(),
            answer.getLastSavedAt()));
      }
    }
    int answered = (int) answerViews.stream().filter(v -> !v.answerKeys().isEmpty()).count();
    return new CompetitionExamAttemptVO(
        attempt.getId(), attempt.getStatus(), now, assignment.getStartAt(), assignment.getEndAt(),
        remaining(now, assignment.getEndAt()), assignment.getQuestionCount(), answered, answerViews);
  }

  private List<String> normalizeAndValidateKeys(List<String> submitted, String optionsJson) {
    Set<String> valid = new LinkedHashSet<>();
    JsonNode options = parseTree(optionsJson);
    if (!options.isArray()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "题目选项快照格式异常");
    }
    options.forEach(option -> valid.add(option.path("key").asText()));
    LinkedHashSet<String> result = new LinkedHashSet<>();
    if (submitted != null) {
      for (String raw : submitted) {
        String key = raw == null ? "" : raw.trim();
        if (!valid.contains(key)) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "答案包含无效选项");
        }
        result.add(key);
      }
    }
    return new ArrayList<>(result);
  }

  private CompetitionExamAnswerVO toAnswerVO(
      CompetitionTheoryAnswerEntity current,
      int acceptedVersion,
      LocalDateTime savedAt,
      List<String> keys,
      boolean marked) {
    return new CompetitionExamAnswerVO(
        current == null ? null : current.getQuestionSnapshotId(), keys, marked,
        acceptedVersion, savedAt);
  }

  private CompetitionExamSubmissionVO submissionView(CompetitionTheoryAttemptEntity attempt) {
    boolean submitted = isTerminal(attempt.getStatus());
    return new CompetitionExamSubmissionVO(
        attempt.getId(), attempt.getStatus(), attempt.getSubmitType(), attempt.getSubmittedAt(), submitted);
  }

  private JsonNode parseTree(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "题目选项快照格式异常");
    }
  }

  private List<String> parseKeys(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> result = objectMapper.readValue(json, STRING_LIST);
      return result == null ? List.of() : result;
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答案快照格式异常");
    }
  }

  private String writeKeys(List<String> keys) {
    try {
      return objectMapper.writeValueAsString(keys);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答案序列化失败");
    }
  }

  private void assertBeforeEnd(CompetitionTheoryExamAssignmentRow assignment, LocalDateTime now) {
    if (!now.isBefore(assignment.getEndAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "考试已经结束，答案不可再修改");
    }
  }

  private long secondsBetween(LocalDateTime from, LocalDateTime to) {
    return Math.max(0, Duration.between(from, to).getSeconds());
  }

  private long remaining(LocalDateTime now, LocalDateTime endAt) {
    return secondsBetween(now, endAt);
  }

  private boolean isTerminal(String status) {
    return status != null && (status.startsWith("submitted_") || "absent".equals(status));
  }

  private CompetitionExamPrincipal currentPlayer() {
    try {
      return CompetitionExamSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Competition exam request has no dedicated exam token");
      throw ex;
    }
  }

  private String fingerprint(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte b : digest) {
        result.append(String.format("%02x", b));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  private void writeAudit(
      CompetitionTheoryAttemptEntity attempt,
      String eventType,
      String sessionStatus,
      String reason,
      Long operatedBy,
      String tokenFingerprint,
      LocalDateTime now) {
    CompetitionTheoryExamSessionTokenAuditEntity audit =
        new CompetitionTheoryExamSessionTokenAuditEntity();
    audit.setTenantId(attempt.getTenantId());
    audit.setPlayerId(attempt.getPlayerId());
    audit.setAssignmentId(attempt.getAssignmentId());
    audit.setAttemptId(attempt.getId());
    audit.setTokenFingerprint(tokenFingerprint);
    audit.setEventType(eventType);
    audit.setSessionStatus(sessionStatus);
    audit.setReason(reason);
    audit.setOperatedBy(operatedBy);
    audit.setOccurredAt(now);
    audit.setDeleted(0);
    if (auditMapper.insert(audit) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试会话审计保存失败");
    }
  }
}
