package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamScoringStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamSessionStatus;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamScoringDecimals;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamScoringException;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionFormalExamScoringService {

  public static final String STUCK_SCORING_MESSAGE = "评分超时未完成，等待重试";

  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalCandidateExamMapper candidateExamMapper;
  private final CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper;
  private final CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper;
  private final CompetitionFormalExamCandidateScoringService candidateScoringService;
  private final Clock clock;
  private final Duration stuckTimeout;

  public CompetitionFormalExamScoringService(
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalExamCandidateScoringService candidateScoringService,
      Clock clock,
      @Value("${platform.competition.formal-exam.scoring.stuck-timeout-minutes:15}")
      long stuckTimeoutMinutes) {
    this.sessionMapper = sessionMapper;
    this.candidateExamMapper = candidateExamMapper;
    this.paperSnapshotMapper = paperSnapshotMapper;
    this.questionSnapshotMapper = questionSnapshotMapper;
    this.candidateScoringService = candidateScoringService;
    this.clock = clock;
    this.stuckTimeout = Duration.ofMinutes(Math.max(stuckTimeoutMinutes, 1));
  }

  public LocalDateTime stuckBefore(LocalDateTime now) {
    return now.minus(stuckTimeout);
  }

  public void retryFromAdmin(Long sessionId) {
    CompetitionFormalExamSessionEntity session = requireSession(sessionId);
    if (!CompetitionFormalExamSessionStatus.FINISHED.getCode().equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前比赛尚未结束，不能评分");
    }
    CompetitionFormalExamScoringStatus scoringStatus =
        CompetitionFormalExamScoringStatus.fromCode(session.getScoringStatus());
    if (scoringStatus == CompetitionFormalExamScoringStatus.COMPLETED) {
      return;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    if (scoringStatus == CompetitionFormalExamScoringStatus.SCORING
        && !isStuck(session, now)) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分正在进行中");
    }
    scoreFinishedSession(sessionId);
    CompetitionFormalExamSessionEntity after = sessionMapper.selectById(sessionId);
    if (after != null
        && CompetitionFormalExamScoringStatus.FAILED.getCode().equals(after.getScoringStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          StringUtils.hasText(after.getScoringError()) ? after.getScoringError() : "正式评分失败");
    }
  }

  public void scoreFinishedSession(Long sessionId) {
    if (sessionId == null) {
      return;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null) {
      return;
    }
    if (!CompetitionFormalExamSessionStatus.FINISHED.getCode().equals(session.getStatus())) {
      log.warn("Formal exam scoring skipped because session is not finished, sessionId={}, status={}",
          sessionId, session.getStatus());
      return;
    }
    CompetitionFormalExamScoringStatus scoringStatus =
        CompetitionFormalExamScoringStatus.fromCode(session.getScoringStatus());
    if (scoringStatus == CompetitionFormalExamScoringStatus.COMPLETED) {
      return;
    }
    if (scoringStatus == CompetitionFormalExamScoringStatus.SCORING && isStuck(session, now)) {
      int reclaimed = sessionMapper.reclaimStuckScoring(
          sessionId, stuckBefore(now), STUCK_SCORING_MESSAGE, now);
      if (reclaimed != 1) {
        return;
      }
      log.warn("Formal exam scoring lock reclaimed after timeout, sessionId={}", sessionId);
    } else if (scoringStatus == CompetitionFormalExamScoringStatus.SCORING) {
      return;
    }
    if (sessionMapper.markScoringIfIdle(sessionId, now, now) != 1) {
      return;
    }
    log.warn(
        "Formal exam scoring acquired, sessionId={}, scoreVersion={}",
        sessionId, FormalExamScoringDecimals.CURRENT_RULE_VERSION);
    try {
      scoreAcquiredSession(sessionId, now);
    } catch (RuntimeException ex) {
      failScoring(sessionId, messageOf(ex), now, ex);
    }
  }

  private void scoreAcquiredSession(Long sessionId, LocalDateTime now) {
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null) {
      throw new FormalExamScoringException("正式比赛运行实例不存在", sessionId, null);
    }
    List<CompetitionFormalCandidateExamEntity> candidates =
        safe(candidateExamMapper.selectByExamSessionId(sessionId));
    String integrity = integrityError(session, candidates);
    if (integrity != null) {
      failScoring(sessionId, integrity, now, null);
      return;
    }
    int failed = 0;
    String firstError = null;
    for (CompetitionFormalCandidateExamEntity candidate : candidates) {
      if (candidate == null || candidate.getId() == null) {
        failed++;
        firstError = firstError == null ? "选手考试实例不完整" : firstError;
        continue;
      }
      try {
        candidateScoringService.scoreCandidate(sessionId, candidate.getId(), now);
      } catch (RuntimeException ex) {
        failed++;
        firstError = firstError == null ? messageOf(ex) : firstError;
        Long questionSnapshotId = ex instanceof FormalExamScoringException scoring
            ? scoring.questionSnapshotId() : null;
        log.error(
            "Formal exam candidate scoring failed, sessionId={}, candidateExamId={}, questionSnapshotId={}, scoreVersion={}",
            sessionId, candidate.getId(), questionSnapshotId,
            FormalExamScoringDecimals.CURRENT_RULE_VERSION, ex);
      }
    }
    if (failed > 0) {
      failScoring(sessionId, firstError == null ? "部分选手评分失败" : firstError, now, null);
      return;
    }
    if (sessionMapper.markScoringCompleted(sessionId, now, now) != 1) {
      log.error("Formal exam scoring complete CAS missed, sessionId={}", sessionId);
      return;
    }
    log.warn(
        "Formal exam scoring completed, sessionId={}, candidateCount={}, scoreVersion={}",
        sessionId, candidates.size(), FormalExamScoringDecimals.CURRENT_RULE_VERSION);
  }

  private String integrityError(
      CompetitionFormalExamSessionEntity session,
      List<CompetitionFormalCandidateExamEntity> candidates) {
    int expected = session.getCandidateTotal() == null ? -1 : session.getCandidateTotal();
    if (candidates.size() != expected) {
      return "选手数量与场次记录不一致";
    }
    for (CompetitionFormalCandidateExamEntity candidate : candidates) {
      if (candidate == null) {
        return "选手考试实例不完整";
      }
      CompetitionFormalCandidateExamStatus status =
          CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus());
      if (status == null || !status.isClosed()) {
        return "存在尚未到达终态的选手，不能评分";
      }
    }
    List<CompetitionFormalExamPaperSnapshotEntity> papers = safe(paperSnapshotMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CompetitionFormalExamPaperSnapshotEntity>()
            .eq(CompetitionFormalExamPaperSnapshotEntity::getExamSessionId, session.getId())
            .eq(CompetitionFormalExamPaperSnapshotEntity::getDeleted, 0)));
    if (papers.isEmpty()) {
      return "正式试卷快照不存在";
    }
    for (CompetitionFormalExamPaperSnapshotEntity paper : papers) {
      List<CompetitionFormalExamQuestionSnapshotEntity> questions =
          safe(questionSnapshotMapper.selectByPaperSnapshotId(paper.getId()));
      if (paper.getQuestionCount() == null || paper.getQuestionCount() != questions.size()) {
        return "正式题目数量与试卷快照不一致";
      }
      if (questions.isEmpty()) {
        return "正式题目快照为空";
      }
      BigDecimal sum = FormalExamScoringDecimals.zero();
      for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
        if (question == null || question.getScore() == null) {
          return "正式题目分值无效";
        }
        sum = sum.add(FormalExamScoringDecimals.money(question.getScore()));
      }
      if (paper.getTotalScore() == null
          || sum.compareTo(FormalExamScoringDecimals.money(paper.getTotalScore())) != 0) {
        return "正式题目分值合计与试卷满分不一致";
      }
    }
    return null;
  }

  private void failScoring(Long sessionId, String message, LocalDateTime now, Throwable cause) {
    String error = truncate(message);
    int updated = sessionMapper.markScoringFailed(sessionId, error, now);
    log.error(
        "Formal exam scoring failed, sessionId={}, scoreVersion={}, scoringError={}, casUpdated={}",
        sessionId, FormalExamScoringDecimals.CURRENT_RULE_VERSION, error, updated, cause);
  }

  private boolean isStuck(CompetitionFormalExamSessionEntity session, LocalDateTime now) {
    return session.getScoringStartedAt() != null
        && !session.getScoringStartedAt().isAfter(stuckBefore(now));
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

  private String messageOf(Throwable ex) {
    if (ex == null) {
      return "正式评分失败";
    }
    if (StringUtils.hasText(ex.getMessage())) {
      return ex.getMessage();
    }
    return ex.getClass().getSimpleName();
  }

  private String truncate(String message) {
    if (!StringUtils.hasText(message)) {
      return "正式评分失败";
    }
    String value = message.trim();
    return value.length() <= 500 ? value : value.substring(0, 500);
  }

  private static <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }
}
