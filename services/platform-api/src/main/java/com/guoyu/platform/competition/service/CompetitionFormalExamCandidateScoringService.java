package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateQuestionScoreEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateResultEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateResultStatus;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamQuestionScoreOutcome;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamQuestionScorerRegistry;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamScoringDecimals;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamScoringException;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateQuestionScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateResultMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.messaging.service.CompetitionScorePipelineOutboxService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionFormalExamCandidateScoringService {

  public enum Outcome {
    SKIPPED,
    SCORED,
    ABSENT
  }

  private final CompetitionFormalCandidateExamMapper candidateExamMapper;
  private final CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper;
  private final CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper;
  private final CompetitionFormalCandidateAnswerMapper answerMapper;
  private final CompetitionFormalCandidateResultMapper resultMapper;
  private final CompetitionFormalCandidateQuestionScoreMapper questionScoreMapper;
  private final FormalExamQuestionScorerRegistry scorerRegistry;
  private CompetitionFormalExamSessionMapper sessionMapper;
  private CompetitionScoreSourceMapper scoreSourceMapper;
  private CompetitionScoreRuleMapper scoreRuleMapper;
  private CompetitionStageContentMapper stageContentMapper;
  private CompetitionScorePipelineOutboxService scorePipelineOutboxService;

  public CompetitionFormalExamCandidateScoringService(
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalExamPaperSnapshotMapper paperSnapshotMapper,
      CompetitionFormalExamQuestionSnapshotMapper questionSnapshotMapper,
      CompetitionFormalCandidateAnswerMapper answerMapper,
      CompetitionFormalCandidateResultMapper resultMapper,
      CompetitionFormalCandidateQuestionScoreMapper questionScoreMapper,
      FormalExamQuestionScorerRegistry scorerRegistry) {
    this.candidateExamMapper = candidateExamMapper;
    this.paperSnapshotMapper = paperSnapshotMapper;
    this.questionSnapshotMapper = questionSnapshotMapper;
    this.answerMapper = answerMapper;
    this.resultMapper = resultMapper;
    this.questionScoreMapper = questionScoreMapper;
    this.scorerRegistry = scorerRegistry;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Outcome scoreCandidate(Long sessionId, Long candidateExamId, LocalDateTime now) {
    return scoreCandidate(sessionId, candidateExamId, 1, null, now);
  }

  @Autowired(required = false)
  public void setScoreSourceDependencies(CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionScoreSourceMapper scoreSourceMapper,
      CompetitionScorePipelineOutboxService scorePipelineOutboxService) {
    this.sessionMapper = sessionMapper;
    this.scoreSourceMapper = scoreSourceMapper;
    this.scorePipelineOutboxService = scorePipelineOutboxService;
  }

  @Autowired(required = false)
  public void setScoreRuleMapper(CompetitionScoreRuleMapper scoreRuleMapper) {
    this.scoreRuleMapper = scoreRuleMapper;
  }

  @Autowired(required = false)
  public void setStageContentMapper(CompetitionStageContentMapper stageContentMapper) {
    this.stageContentMapper = stageContentMapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Outcome scoreCandidate(Long sessionId, Long candidateExamId, int revisionNo,
      String correctionReason, LocalDateTime now) {
    if (sessionId == null || candidateExamId == null || now == null) {
      throw new FormalExamScoringException("评分对象不能为空", sessionId, candidateExamId);
    }
    CompetitionFormalCandidateExamEntity candidate = candidateExamMapper.selectById(candidateExamId);
    if (candidate == null || !Objects.equals(sessionId, candidate.getExamSessionId())) {
      throw new FormalExamScoringException("选手考试实例不存在", sessionId, candidateExamId);
    }
    CompetitionFormalCandidateExamStatus status =
        CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus());
    if (status == null || !status.isClosed()) {
      throw new FormalExamScoringException(
          "选手考试尚未到达终态，不能评分: " + candidate.getStatus(),
          sessionId, candidateExamId);
    }
    CompetitionFormalCandidateResultEntity current = resultMapper.selectByCandidateExamId(candidate.getId());
    if (current != null && (Objects.equals(current.getRevisionNo(), revisionNo)
        || (current.getRevisionNo() == null && revisionNo == 1))
        && isComplete(candidate, status, current)) {
      return status == CompetitionFormalCandidateExamStatus.ABSENT ? Outcome.ABSENT : Outcome.SKIPPED;
    }
    if (revisionNo > 1 && (correctionReason == null || correctionReason.isBlank())) {
      throw new FormalExamScoringException("重新判分必须填写原因", sessionId, candidateExamId);
    }
    prepareRevision(candidateExamId, revisionNo, current, now);
    if (status == CompetitionFormalCandidateExamStatus.ABSENT) {
      insertAbsentResult(candidate, revisionNo, correctionReason, now);
      log.warn(
          "Formal exam candidate marked absent result, sessionId={}, candidateExamId={}, scoreVersion={}",
          sessionId, candidateExamId, FormalExamScoringDecimals.CURRENT_RULE_VERSION);
      return Outcome.ABSENT;
    }
    scoreSubmitted(candidate, revisionNo, correctionReason, now);
    log.warn(
        "Formal exam candidate scored, sessionId={}, candidateExamId={}, scoreVersion={}",
        sessionId, candidateExamId, FormalExamScoringDecimals.CURRENT_RULE_VERSION);
    return Outcome.SCORED;
  }

  private boolean isComplete(CompetitionFormalCandidateExamEntity candidate,
      CompetitionFormalCandidateExamStatus status, CompetitionFormalCandidateResultEntity result) {
    if (!Objects.equals(FormalExamScoringDecimals.CURRENT_RULE_VERSION, result.getScoreVersion())) {
      return false;
    }
    List<CompetitionFormalCandidateQuestionScoreEntity> loadedScores = result.getId() == null
        ? null : questionScoreMapper.selectByCandidateResultId(result.getId());
    List<CompetitionFormalCandidateQuestionScoreEntity> scores = loadedScores == null
        ? safe(questionScoreMapper.selectByCandidateExamId(candidate.getId())) : safe(loadedScores);
    if (status == CompetitionFormalCandidateExamStatus.ABSENT) {
      return CompetitionFormalCandidateResultStatus.ABSENT.getCode().equals(result.getResultStatus())
          && result.getTotalScore() == null
          && result.getFullScore() == null
          && result.getQuestionCount() == null
          && result.getAnsweredCount() == null
          && result.getCorrectCount() == null
          && result.getWrongCount() == null
          && result.getUnansweredCount() == null
          && scores.isEmpty();
    }
    if (!CompetitionFormalCandidateResultStatus.SCORED.getCode().equals(result.getResultStatus())
        || result.getTotalScore() == null
        || result.getFullScore() == null
        || result.getQuestionCount() == null) {
      return false;
    }
    List<CompetitionFormalExamQuestionSnapshotEntity> questions =
        safe(questionSnapshotMapper.selectByPaperSnapshotId(candidate.getPaperSnapshotId()));
    if (scores.size() != questions.size() || scores.size() != result.getQuestionCount()) {
      return false;
    }
    BigDecimal awarded = FormalExamScoringDecimals.zero();
    BigDecimal full = FormalExamScoringDecimals.zero();
    for (CompetitionFormalCandidateQuestionScoreEntity score : scores) {
      if (score.getAwardedScore() == null || score.getMaxScore() == null) {
        return false;
      }
      awarded = awarded.add(FormalExamScoringDecimals.money(score.getAwardedScore()));
      full = full.add(FormalExamScoringDecimals.money(score.getMaxScore()));
    }
    return awarded.compareTo(FormalExamScoringDecimals.money(result.getTotalScore())) == 0
        && full.compareTo(FormalExamScoringDecimals.money(result.getFullScore())) == 0;
  }

  private void scoreSubmitted(CompetitionFormalCandidateExamEntity candidate, int revisionNo,
      String correctionReason, LocalDateTime now) {
    CompetitionFormalExamPaperSnapshotEntity paper =
        paperSnapshotMapper.selectById(candidate.getPaperSnapshotId());
    if (paper == null || !Objects.equals(paper.getExamSessionId(), candidate.getExamSessionId())) {
      throw new FormalExamScoringException(
          "正式试卷快照不存在", candidate.getExamSessionId(), candidate.getId());
    }
    List<CompetitionFormalExamQuestionSnapshotEntity> questions =
        safe(questionSnapshotMapper.selectByPaperSnapshotId(paper.getId()));
    if (questions.isEmpty()) {
      throw new FormalExamScoringException(
          "正式题目快照为空", candidate.getExamSessionId(), candidate.getId());
    }
    if (paper.getQuestionCount() == null || paper.getQuestionCount() != questions.size()) {
      throw new FormalExamScoringException(
          "正式题目数量与试卷快照不一致", candidate.getExamSessionId(), candidate.getId());
    }
    BigDecimal paperFull = sumQuestionScores(questions);
    if (paper.getTotalScore() == null
        || paperFull.compareTo(FormalExamScoringDecimals.money(paper.getTotalScore())) != 0) {
      throw new FormalExamScoringException(
          "正式题目分值合计与试卷满分不一致", candidate.getExamSessionId(), candidate.getId());
    }
    Map<Long, CompetitionFormalCandidateAnswerEntity> answers = answersByQuestion(candidate.getId());
    int answeredCount = 0;
    int correctCount = 0;
    int wrongCount = 0;
    BigDecimal totalScore = FormalExamScoringDecimals.zero();
    CompetitionFormalCandidateResultEntity result =
        newResult(candidate, revisionNo, correctionReason, now);
    result.setResultStatus(CompetitionFormalCandidateResultStatus.SCORED.getCode());
    result.setFullScore(paperFull);
    result.setQuestionCount(questions.size());
    if (resultMapper.insert(result) != 1) {
      throw new FormalExamScoringException(
          "保存正式成绩失败", candidate.getExamSessionId(), candidate.getId());
    }
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      if (question == null || question.getId() == null) {
        throw new FormalExamScoringException(
            "正式题目快照不完整", candidate.getExamSessionId(), candidate.getId());
      }
      CompetitionFormalCandidateAnswerEntity answer = answers.get(question.getId());
      FormalExamQuestionScoreOutcome outcome;
      try {
        outcome = scorerRegistry.score(question, answer);
      } catch (FormalExamScoringException ex) {
        throw new FormalExamScoringException(
            ex.getMessage(), candidate.getExamSessionId(), candidate.getId(), question.getId(), ex);
      }
      insertQuestionScore(result, candidate, question, outcome, now);
      totalScore = totalScore.add(outcome.awardedScore());
      if (outcome.answered()) {
        answeredCount++;
        if (outcome.correct()) {
          correctCount++;
        } else {
          wrongCount++;
        }
      }
    }
    if (correctCount + wrongCount != answeredCount) {
      throw new FormalExamScoringException(
          "正式成绩作答统计不一致", candidate.getExamSessionId(), candidate.getId());
    }
    result.setTotalScore(totalScore);
    result.setAnsweredCount(answeredCount);
    result.setCorrectCount(correctCount);
    result.setWrongCount(wrongCount);
    result.setUnansweredCount(questions.size() - answeredCount);
    if (resultMapper.updateById(result) != 1) {
      throw new FormalExamScoringException(
          "更新正式成绩汇总失败", candidate.getExamSessionId(), candidate.getId());
    }
    upsertTheorySource(candidate, paper, result, now);
    if (candidate.getAnsweredCount() != null && candidate.getAnsweredCount() != answeredCount) {
      log.warn(
          "Formal exam answeredCount mismatch, sessionId={}, candidateExamId={}, runtime={}, scored={}",
          candidate.getExamSessionId(), candidate.getId(),
          candidate.getAnsweredCount(), answeredCount);
    }
  }

  private void insertAbsentResult(CompetitionFormalCandidateExamEntity candidate, int revisionNo,
      String correctionReason, LocalDateTime now) {
    CompetitionFormalCandidateResultEntity result =
        newResult(candidate, revisionNo, correctionReason, now);
    result.setResultStatus(CompetitionFormalCandidateResultStatus.ABSENT.getCode());
    result.setFullScore(null);
    result.setTotalScore(null);
    result.setQuestionCount(null);
    result.setAnsweredCount(null);
    result.setCorrectCount(null);
    result.setWrongCount(null);
    result.setUnansweredCount(null);
    if (resultMapper.insert(result) != 1) {
      throw new FormalExamScoringException(
          "保存缺考成绩失败", candidate.getExamSessionId(), candidate.getId());
    }
  }

  private CompetitionFormalCandidateResultEntity newResult(
      CompetitionFormalCandidateExamEntity candidate, int revisionNo,
      String correctionReason, LocalDateTime now) {
    CompetitionFormalCandidateResultEntity result = new CompetitionFormalCandidateResultEntity();
    result.setId(IdWorker.getId());
    result.setTenantId(candidate.getTenantId());
    result.setExamSessionId(candidate.getExamSessionId());
    result.setCandidateExamId(candidate.getId());
    result.setRevisionNo(revisionNo);
    result.setScoreVersion(FormalExamScoringDecimals.CURRENT_RULE_VERSION);
    result.setCurrentFlag(1);
    result.setCorrectionReason(correctionReason);
    result.setScoredTime(now);
    result.setCreatedAt(now);
    result.setUpdatedAt(now);
    result.setDeleted(0);
    return result;
  }

  private void insertQuestionScore(
      CompetitionFormalCandidateResultEntity result,
      CompetitionFormalCandidateExamEntity candidate,
      CompetitionFormalExamQuestionSnapshotEntity question,
      FormalExamQuestionScoreOutcome outcome,
      LocalDateTime now) {
    CompetitionFormalCandidateQuestionScoreEntity score =
        new CompetitionFormalCandidateQuestionScoreEntity();
    score.setId(IdWorker.getId());
    score.setTenantId(candidate.getTenantId());
    score.setCandidateResultId(result.getId());
    score.setExamSessionId(candidate.getExamSessionId());
    score.setCandidateExamId(candidate.getId());
    score.setQuestionSnapshotId(question.getId());
    score.setQuestionOrder(question.getQuestionOrder());
    score.setQuestionType(question.getQuestionType());
    score.setAnswerVersion(outcome.answerVersion());
    score.setIsAnswered(outcome.answered() ? 1 : 0);
    score.setIsCorrect(outcome.correct() ? 1 : 0);
    score.setMaxScore(outcome.maxScore());
    score.setAwardedScore(outcome.awardedScore());
    score.setScoringRuleVersion(FormalExamScoringDecimals.CURRENT_RULE_VERSION);
    score.setScoredTime(now);
    score.setCreatedAt(now);
    score.setUpdatedAt(now);
    score.setDeleted(0);
    if (questionScoreMapper.insert(score) != 1) {
      throw new FormalExamScoringException(
          "保存逐题评分失败", candidate.getExamSessionId(), candidate.getId(), question.getId());
    }
  }

  private void prepareRevision(Long candidateExamId, int revisionNo,
      CompetitionFormalCandidateResultEntity current, LocalDateTime now) {
    if (current == null) return;
    if (Objects.equals(current.getRevisionNo(), revisionNo)) {
      questionScoreMapper.delete(new LambdaQueryWrapper<CompetitionFormalCandidateQuestionScoreEntity>()
          .eq(CompetitionFormalCandidateQuestionScoreEntity::getCandidateResultId, current.getId()));
      resultMapper.deleteById(current.getId());
      return;
    }
    resultMapper.clearCurrent(candidateExamId, now);
  }

  private void upsertTheorySource(CompetitionFormalCandidateExamEntity candidate,
      CompetitionFormalExamPaperSnapshotEntity paper,
      CompetitionFormalCandidateResultEntity result, LocalDateTime now) {
    if (sessionMapper == null || scoreSourceMapper == null) return;
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(candidate.getExamSessionId());
    if (session == null) {
      throw new FormalExamScoringException(
          "正式成绩无法绑定冻结计分规则", candidate.getExamSessionId(), candidate.getId());
    }
    Long scoreRuleId = resolveAndFreezeScoreRule(candidate, paper, session, now);
    String reference = "candidate-result:" + result.getId();
    CompetitionScoreSourceEntity same = scoreSourceMapper.selectOne(
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getTenantId, candidate.getTenantId())
            .eq(CompetitionScoreSourceEntity::getSourceSystem, "competition-formal-exam")
            .eq(CompetitionScoreSourceEntity::getSourceReference, reference)
            .eq(CompetitionScoreSourceEntity::getDeleted, 0).last("LIMIT 1"));
    if (same != null) return;
    CompetitionScoreSourceEntity current = scoreSourceMapper.selectOne(
        new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
            .eq(CompetitionScoreSourceEntity::getEventId, session.getEventId())
            .eq(CompetitionScoreSourceEntity::getStageId, session.getStageId())
            .eq(CompetitionScoreSourceEntity::getEventWorkTypeId, session.getEventWorkTypeId())
            .eq(session.getEventWorkTypeLevelId() != null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId,
                session.getEventWorkTypeLevelId())
            .isNull(session.getEventWorkTypeLevelId() == null,
                CompetitionScoreSourceEntity::getEventWorkTypeLevelId)
            .eq(CompetitionScoreSourceEntity::getContestantMemberId,
                candidate.getContestantMemberId())
            .eq(CompetitionScoreSourceEntity::getSourceType, "theory")
            .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1)
            .eq(CompetitionScoreSourceEntity::getDeleted, 0).last("LIMIT 1"));
    if (current != null) {
      current.setCurrentFlag(0);
      current.setUpdatedAt(now);
      scoreSourceMapper.updateById(current);
    }
    CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity();
    source.setId(IdWorker.getId());
    source.setTenantId(candidate.getTenantId());
    source.setEventId(session.getEventId());
    source.setStageId(session.getStageId());
    source.setEventWorkTypeId(session.getEventWorkTypeId());
    source.setEventWorkTypeLevelId(session.getEventWorkTypeLevelId());
    source.setContestantMemberId(candidate.getContestantMemberId());
    source.setSourceType("theory");
    source.setRawScore(result.getTotalScore());
    source.setConfirmationStatus("confirmed");
    source.setSourceSystem("competition-formal-exam");
    source.setSourceReference(reference);
    source.setScoreRuleId(scoreRuleId);
    source.setRevisionNo(current == null ? 1 : current.getRevisionNo() + 1);
    source.setCurrentFlag(1);
    source.setAchievedAt(result.getScoredTime());
    source.setConfirmedAt(now);
    source.setConfirmedBy(candidate.getUpdatedBy() == null ? 0L : candidate.getUpdatedBy());
    source.setCreatedBy(source.getConfirmedBy());
    source.setUpdatedBy(source.getConfirmedBy());
    source.setCreatedAt(now);
    source.setUpdatedAt(now);
    source.setDeleted(0);
    if (scoreSourceMapper.insert(source) != 1) {
      throw new FormalExamScoringException(
          "理论成绩源保存失败", candidate.getExamSessionId(), candidate.getId());
    }
    if (scorePipelineOutboxService != null) {
      scorePipelineOutboxService.enqueue(source, source.getConfirmedBy());
    }
  }

  private Long resolveAndFreezeScoreRule(
      CompetitionFormalCandidateExamEntity candidate,
      CompetitionFormalExamPaperSnapshotEntity paper,
      CompetitionFormalExamSessionEntity session,
      LocalDateTime now) {
    // A theory-only stage is scored from the immutable question snapshot.  There is no
    // cross-subject weighting to freeze, so the historical score-rule reference is intentionally
    // left empty.  This check deliberately precedes the paper snapshot's legacy scoreRuleId so a
    // retry cannot reintroduce the retired cross-subject rule dependency.
    if (isTheoryOnlyStage(session.getStageId())) return null;
    if (paper.getScoreRuleId() != null) return paper.getScoreRuleId();
    // Mixed theory/simulation/practical stages still require a published rule.
    if (scoreRuleMapper == null) {
      throw new FormalExamScoringException(
          "正式成绩无法绑定冻结计分规则", candidate.getExamSessionId(), candidate.getId());
    }
    List<CompetitionScoreRuleEntity> rules = scoreRuleMapper.selectList(
        new LambdaQueryWrapper<CompetitionScoreRuleEntity>()
            .eq(CompetitionScoreRuleEntity::getEventId, session.getEventId())
            .eq(CompetitionScoreRuleEntity::getStageId, session.getStageId())
            .eq(CompetitionScoreRuleEntity::getEventWorkTypeId, session.getEventWorkTypeId())
            .eq(session.getEventWorkTypeLevelId() != null,
                CompetitionScoreRuleEntity::getEventWorkTypeLevelId,
                session.getEventWorkTypeLevelId())
            .isNull(session.getEventWorkTypeLevelId() == null,
                CompetitionScoreRuleEntity::getEventWorkTypeLevelId)
            .eq(CompetitionScoreRuleEntity::getStatus, "published")
            .eq(CompetitionScoreRuleEntity::getDeleted, 0));
    if (rules == null || rules.size() != 1) {
      throw new FormalExamScoringException(
          "正式成绩需要当前赛段存在唯一的已发布计分规则",
          candidate.getExamSessionId(), candidate.getId());
    }
    Long resolvedRuleId = rules.getFirst().getId();
    paper.setScoreRuleId(resolvedRuleId);
    paper.setUpdatedAt(now);
    paper.setUpdatedBy(candidate.getUpdatedBy());
    if (paperSnapshotMapper.updateById(paper) != 1) {
      throw new FormalExamScoringException(
          "冻结正式成绩计分规则失败", candidate.getExamSessionId(), candidate.getId());
    }
    log.warn("Formal exam scoring rule frozen during legacy recovery, sessionId={}, "
            + "candidateExamId={}, paperSnapshotId={}, scoreRuleId={}",
        candidate.getExamSessionId(), candidate.getId(), paper.getId(), resolvedRuleId);
    return resolvedRuleId;
  }

  private boolean isTheoryOnlyStage(Long stageId) {
    if (stageContentMapper == null || stageId == null) return false;
    List<CompetitionStageContentEntity> contents = stageContentMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageContentEntity>()
            .eq(CompetitionStageContentEntity::getStageId, stageId)
            .eq(CompetitionStageContentEntity::getStatus, "enabled")
            .eq(CompetitionStageContentEntity::getDeleted, 0));
    return contents != null && !contents.isEmpty()
        && contents.stream().allMatch(content -> content != null
            && "theory".equalsIgnoreCase(content.getContentType()));
  }

  private Map<Long, CompetitionFormalCandidateAnswerEntity> answersByQuestion(Long candidateExamId) {
    Map<Long, CompetitionFormalCandidateAnswerEntity> answers = new HashMap<>();
    for (CompetitionFormalCandidateAnswerEntity answer : safe(answerMapper.selectByCandidateExamId(candidateExamId))) {
      if (answer == null || answer.getQuestionSnapshotId() == null) {
        continue;
      }
      if (answers.put(answer.getQuestionSnapshotId(), answer) != null) {
        throw new FormalExamScoringException("选手存在重复正式答案", null, candidateExamId);
      }
    }
    return answers;
  }

  private BigDecimal sumQuestionScores(List<CompetitionFormalExamQuestionSnapshotEntity> questions) {
    BigDecimal total = FormalExamScoringDecimals.zero();
    for (CompetitionFormalExamQuestionSnapshotEntity question : questions) {
      if (question == null || question.getScore() == null) {
        throw new FormalExamScoringException("正式题目分值无效");
      }
      total = total.add(FormalExamScoringDecimals.money(question.getScore()));
    }
    return total;
  }

  private static <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }
}
