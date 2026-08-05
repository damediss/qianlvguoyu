package com.guoyu.platform.competition.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.domain.CompetitionTheoryObjectiveScorer;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionTokenAuditEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionTokenAuditMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.service.CompetitionContestantScopeValidator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionTheorySubmissionService {
  private static final String SOURCE_SYSTEM = "competition-theory-exam";
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

  private final CompetitionTheoryAttemptMapper attemptMapper;
  private final CompetitionTheoryAnswerMapper answerMapper;
  private final CompetitionTheoryPaperQuestionMapper questionMapper;
  private final CompetitionTheoryPaperSnapshotMapper paperMapper;
  private final CompetitionTheoryExamSessionMapper sessionMapper;
  private final CompetitionScoreSourceMapper scoreSourceMapper;
  private final CompetitionTheoryExamSessionTokenAuditMapper auditMapper;
  private final CompetitionContestantScopeValidator contestantScopeValidator;
  private final ObjectMapper objectMapper;
  private final CompetitionTheoryObjectiveScorer scorer = new CompetitionTheoryObjectiveScorer();

  public CompetitionTheorySubmissionService(
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryAnswerMapper answerMapper,
      CompetitionTheoryPaperQuestionMapper questionMapper,
      CompetitionTheoryPaperSnapshotMapper paperMapper,
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionScoreSourceMapper scoreSourceMapper,
      CompetitionTheoryExamSessionTokenAuditMapper auditMapper,
      CompetitionContestantScopeValidator contestantScopeValidator,
      ObjectMapper objectMapper) {
    this.attemptMapper = attemptMapper;
    this.answerMapper = answerMapper;
    this.questionMapper = questionMapper;
    this.paperMapper = paperMapper;
    this.sessionMapper = sessionMapper;
    this.scoreSourceMapper = scoreSourceMapper;
    this.auditMapper = auditMapper;
    this.contestantScopeValidator = contestantScopeValidator;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public CompetitionTheoryAttemptEntity submit(
      Long attemptId,
      String submitType,
      String requestId,
      Long forcedBy,
      String reason,
      LocalDateTime now) {
    CompetitionTheoryAttemptEntity attempt = requireAttemptForUpdate(attemptId);
    if (isTerminal(attempt.getStatus())) {
      return attempt;
    }
    validateAttemptScope(attempt);
    if (!"in_progress".equals(attempt.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前考试状态不可交卷");
    }
    CompetitionTheoryExamSessionEntity session = sessionMapper.selectById(attempt.getSessionId());
    if (session == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试场次不存在");
    }
    if ("manual".equals(submitType) && now.isBefore(session.getStartAt().plusMinutes(30))) {
      throw new BusinessException(ErrorCode.CONFLICT, "开考满30分钟后方可主动交卷");
    }
    if (attemptMapper.claimSubmission(
        attemptId, submitType, requestId, forcedBy, reason, now) != 1) {
      return requireAttemptForUpdate(attemptId);
    }

    BigDecimal total = scoreAttempt(attempt, now);
    CompetitionScoreSourceEntity source = createConfirmedSource(attempt, total, now);
    String terminalStatus = switch (submitType) {
      case "manual" -> "submitted_manual";
      case "auto" -> "submitted_auto";
      case "forced" -> "submitted_forced";
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "未知交卷类型");
    };
    if (attemptMapper.finishSubmission(attemptId, terminalStatus, total, source.getId(), now) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "交卷状态已被其他请求推进");
    }
    writeAudit(attempt, "auto".equals(submitType) ? "auto_submit"
        : "forced".equals(submitType) ? "force_submit" : "submit",
        "submitted", reason, forcedBy, requestId, now);
    return requireAttemptForUpdate(attemptId);
  }

  @Transactional
  public CompetitionTheoryAttemptEntity finalizeAbsence(Long attemptId, LocalDateTime now) {
    CompetitionTheoryAttemptEntity attempt = requireAttemptForUpdate(attemptId);
    if (isTerminal(attempt.getStatus())) {
      return attempt;
    }
    validateAttemptScope(attempt);
    if ("in_progress".equals(attempt.getStatus()) || "submitting".equals(attempt.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "答题中的选手不能结算为缺考");
    }
    CompetitionScoreSourceEntity source = createConfirmedSource(attempt, BigDecimal.ZERO, now);
    if (attemptMapper.finishAbsent(attemptId, source.getId(), now) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "缺考状态已被其他请求推进");
    }
    writeAudit(attempt, "absent", "absent", "场次结束未开始考试", 0L, null, now);
    return requireAttemptForUpdate(attemptId);
  }

  private BigDecimal scoreAttempt(CompetitionTheoryAttemptEntity attempt, LocalDateTime now) {
    List<CompetitionTheoryPaperQuestionEntity> questions =
        questionMapper.selectByPaper(attempt.getPaperSnapshotId());
    if (questions == null || questions.isEmpty()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试试卷没有题目快照");
    }
    List<CompetitionTheoryAnswerEntity> saved = answerMapper.selectByAttempt(attempt.getId());
    Map<Long, CompetitionTheoryAnswerEntity> byQuestion = new HashMap<>();
    if (saved != null) {
      saved.forEach(answer -> byQuestion.put(answer.getQuestionSnapshotId(), answer));
    }
    BigDecimal total = BigDecimal.ZERO;
    for (CompetitionTheoryPaperQuestionEntity question : questions) {
      CompetitionTheoryAnswerEntity answer = byQuestion.get(question.getId());
      List<String> submitted = answer == null ? List.of() : parseKeys(answer.getAnswerJson());
      List<String> standard = parseKeys(question.getCorrectAnswersJson());
      BigDecimal awarded = "multi".equals(question.getQuestionType())
          ? scorer.scoreMultipleChoice(standard, submitted, question.getScore())
          : scorer.scoreExactAnswer(first(standard), first(submitted), question.getScore());
      total = total.add(awarded);
      if (answer == null) {
        answer = new CompetitionTheoryAnswerEntity();
        answer.setTenantId(attempt.getTenantId());
        answer.setAttemptId(attempt.getId());
        answer.setPaperSnapshotId(attempt.getPaperSnapshotId());
        answer.setQuestionSnapshotId(question.getId());
        answer.setAnswerJson(null);
        answer.setMarkedForReview(0);
        answer.setSaveVersion(1);
        answer.setLastRequestId("score:" + attempt.getId() + ":" + question.getId());
        answer.setLastSavedAt(now);
        answer.setQuestionScore(question.getScore());
        answer.setScoringRuleCode(question.getScoringRuleCode());
        answer.setScoringRuleVersion(question.getScoringRuleVersion());
        answer.setDeleted(0);
      }
      answer.setAwardedScore(awarded);
      answer.setCorrectFlag(awarded.compareTo(question.getScore()) == 0 ? 1 : 0);
      answer.setScoredAt(now);
      int changed = answer.getId() == null ? answerMapper.insert(answer) : answerMapper.updateById(answer);
      if (changed != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "逐题判分结果保存失败");
      }
    }
    return total;
  }

  private CompetitionScoreSourceEntity createConfirmedSource(
      CompetitionTheoryAttemptEntity attempt, BigDecimal score, LocalDateTime now) {
    String reference = "theory-attempt:" + attempt.getId();
    List<CompetitionScoreSourceEntity> existingSources = findSourcesByReference(reference);
    if (!existingSources.isEmpty()) {
      existingSources.forEach(source -> ensureSourceScope(source, attempt));
      return existingSources.getFirst();
    }
    CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity();
    source.setTenantId(attempt.getTenantId());
    source.setEventId(attempt.getEventId());
    source.setStageId(attempt.getStageId());
    source.setEventWorkTypeId(attempt.getEventWorkTypeId());
    source.setContestantMemberId(attempt.getContestantMemberId());
    source.setSourceType("theory");
    source.setRawScore(score);
    source.setConfirmationStatus("confirmed");
    source.setSourceSystem(SOURCE_SYSTEM);
    source.setSourceReference(reference);
    source.setScoreRuleId(resolveScoreRuleId(attempt.getPaperSnapshotId()));
    source.setRevisionNo(1);
    source.setCurrentFlag(1);
    source.setAchievedAt(now);
    source.setConfirmedAt(now);
    source.setConfirmedBy(0L);
    source.setDeleted(0);
    if (scoreSourceMapper.insert(source) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "理论成绩来源保存失败");
    }
    return source;
  }

  private void validateAttemptScope(CompetitionTheoryAttemptEntity attempt) {
    ensureSourceReferenceScope(attempt);
  }

  private void ensureSourceReferenceScope(CompetitionTheoryAttemptEntity attempt) {
    String reference = "theory-attempt:" + attempt.getId();
    findSourcesByReference(reference).forEach(source -> ensureSourceScope(source, attempt));
  }

  private List<CompetitionScoreSourceEntity> findSourcesByReference(String reference) {
    List<CompetitionScoreSourceEntity> sources = scoreSourceMapper.selectList(
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getSourceSystem, SOURCE_SYSTEM)
            .eq(CompetitionScoreSourceEntity::getSourceReference, reference)
            .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1)
            .eq(CompetitionScoreSourceEntity::getDeleted, 0));
    return sources == null ? List.of() : sources;
  }

  private void ensureSourceScope(
      CompetitionScoreSourceEntity source, CompetitionTheoryAttemptEntity attempt) {
    boolean sameScope = Objects.equals(source.getTenantId(), attempt.getTenantId())
        && Objects.equals(source.getEventId(), attempt.getEventId())
        && Objects.equals(source.getStageId(), attempt.getStageId())
        && Objects.equals(source.getEventWorkTypeId(), attempt.getEventWorkTypeId())
        && Objects.equals(source.getContestantMemberId(), attempt.getContestantMemberId());
    if (!sameScope) {
      log.warn("Theory score source reference scope conflict, sourceReference={}, attemptId={}",
          source.getSourceReference(), attempt.getId());
      throw new BusinessException(ErrorCode.CONFLICT, "理论成绩来源引用与当前业务范围冲突");
    }
  }

  private Long resolveScoreRuleId(Long paperSnapshotId) {
    com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity paper =
        paperMapper.selectById(paperSnapshotId);
    if (paper == null || paper.getScoreRuleId() == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试卷评分规则引用不存在");
    }
    return paper.getScoreRuleId();
  }

  private CompetitionTheoryAttemptEntity requireAttemptForUpdate(Long attemptId) {
    // A plain read is only for locating the scope. The profile lock is always acquired before
    // the attempt lock used for the actual write transaction.
    CompetitionTheoryAttemptEntity attempt = attemptMapper.selectById(attemptId);
    boolean alreadyLocked = false;
    if (attempt == null) {
      // Compatibility fallback for callers/tests that expose only the locking lookup. A real
      // record takes the profile-first path above.
      attempt = attemptMapper.selectForUpdateById(attemptId);
      alreadyLocked = attempt != null;
    }
    if (attempt == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试作答记录不存在");
    }
    if (TenantContextHolder.getTenantId() == null) {
      // Direct service tests and non-HTTP callers without a tenant context retain the read-only
      // compatibility path; every real request/scheduler path sets the context and takes FOR UPDATE.
      contestantScopeValidator.validate(
          attempt.getTenantId(), attempt.getEventId(), attempt.getStageId(),
          attempt.getEventWorkTypeId(), attempt.getContestantMemberId());
    } else {
      contestantScopeValidator.validateForUpdate(
          attempt.getTenantId(), attempt.getEventId(), attempt.getStageId(),
          attempt.getEventWorkTypeId(), attempt.getContestantMemberId());
    }
    if (alreadyLocked) {
      return attempt;
    }
    CompetitionTheoryAttemptEntity locked = attemptMapper.selectForUpdateById(attemptId);
    if (locked == null || !sameAttemptScope(attempt, locked)) {
      log.warn("Theory attempt scope changed while acquiring profile-first lock, attemptId={}", attemptId);
      throw new BusinessException(ErrorCode.CONFLICT, "考试范围已发生变化，请重试");
    }
    return locked;
  }

  private boolean sameAttemptScope(
      CompetitionTheoryAttemptEntity before, CompetitionTheoryAttemptEntity after) {
    return Objects.equals(before.getTenantId(), after.getTenantId())
        && Objects.equals(before.getEventId(), after.getEventId())
        && Objects.equals(before.getStageId(), after.getStageId())
        && Objects.equals(before.getEventWorkTypeId(), after.getEventWorkTypeId())
        && Objects.equals(before.getContestantMemberId(), after.getContestantMemberId())
        && Objects.equals(before.getContestantProfileId(), after.getContestantProfileId())
        && Objects.equals(before.getAssignmentId(), after.getAssignmentId());
  }

  private List<String> parseKeys(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> keys = objectMapper.readValue(json, STRING_LIST);
      return keys == null ? List.of() : keys;
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试答案快照格式异常");
    }
  }

  private String first(List<String> values) {
    return values == null || values.isEmpty() ? null : values.getFirst();
  }

  private boolean isTerminal(String status) {
    return status != null && (status.startsWith("submitted_") || "absent".equals(status));
  }

  private void writeAudit(
      CompetitionTheoryAttemptEntity attempt,
      String eventType,
      String sessionStatus,
      String reason,
      Long operatedBy,
      String requestId,
      LocalDateTime now) {
    CompetitionTheoryExamSessionTokenAuditEntity audit =
        new CompetitionTheoryExamSessionTokenAuditEntity();
    audit.setTenantId(attempt.getTenantId());
    audit.setPlayerId(attempt.getPlayerId());
    audit.setAssignmentId(attempt.getAssignmentId());
    audit.setAttemptId(attempt.getId());
    audit.setEventType(eventType);
    audit.setSessionStatus(sessionStatus);
    audit.setReason(reason);
    audit.setOperatedBy(operatedBy);
    audit.setOccurredAt(now);
    audit.setRequestId(requestId);
    audit.setDeleted(0);
    if (auditMapper.insert(audit) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试会话审计保存失败");
    }
  }
}
