package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateQuestionScoreEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateResultEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    if (isComplete(candidate, status)) {
      return status == CompetitionFormalCandidateExamStatus.ABSENT ? Outcome.ABSENT : Outcome.SKIPPED;
    }
    wipeCurrent(candidateExamId);
    if (status == CompetitionFormalCandidateExamStatus.ABSENT) {
      insertAbsentResult(candidate, now);
      log.warn(
          "Formal exam candidate marked absent result, sessionId={}, candidateExamId={}, scoreVersion={}",
          sessionId, candidateExamId, FormalExamScoringDecimals.CURRENT_RULE_VERSION);
      return Outcome.ABSENT;
    }
    scoreSubmitted(candidate, now);
    log.warn(
        "Formal exam candidate scored, sessionId={}, candidateExamId={}, scoreVersion={}",
        sessionId, candidateExamId, FormalExamScoringDecimals.CURRENT_RULE_VERSION);
    return Outcome.SCORED;
  }

  private boolean isComplete(
      CompetitionFormalCandidateExamEntity candidate, CompetitionFormalCandidateExamStatus status) {
    CompetitionFormalCandidateResultEntity result =
        resultMapper.selectByCandidateExamId(candidate.getId());
    if (result == null) {
      return false;
    }
    if (!Objects.equals(FormalExamScoringDecimals.CURRENT_RULE_VERSION, result.getScoreVersion())) {
      return false;
    }
    List<CompetitionFormalCandidateQuestionScoreEntity> scores =
        safe(questionScoreMapper.selectByCandidateExamId(candidate.getId()));
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

  private void scoreSubmitted(CompetitionFormalCandidateExamEntity candidate, LocalDateTime now) {
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
    CompetitionFormalCandidateResultEntity result = newResult(candidate, now);
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
    if (candidate.getAnsweredCount() != null && candidate.getAnsweredCount() != answeredCount) {
      log.warn(
          "Formal exam answeredCount mismatch, sessionId={}, candidateExamId={}, runtime={}, scored={}",
          candidate.getExamSessionId(), candidate.getId(),
          candidate.getAnsweredCount(), answeredCount);
    }
  }

  private void insertAbsentResult(CompetitionFormalCandidateExamEntity candidate, LocalDateTime now) {
    CompetitionFormalCandidateResultEntity result = newResult(candidate, now);
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
      CompetitionFormalCandidateExamEntity candidate, LocalDateTime now) {
    CompetitionFormalCandidateResultEntity result = new CompetitionFormalCandidateResultEntity();
    result.setId(IdWorker.getId());
    result.setTenantId(candidate.getTenantId());
    result.setExamSessionId(candidate.getExamSessionId());
    result.setCandidateExamId(candidate.getId());
    result.setScoreVersion(FormalExamScoringDecimals.CURRENT_RULE_VERSION);
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

  private void wipeCurrent(Long candidateExamId) {
    questionScoreMapper.delete(new LambdaQueryWrapper<CompetitionFormalCandidateQuestionScoreEntity>()
        .eq(CompetitionFormalCandidateQuestionScoreEntity::getCandidateExamId, candidateExamId));
    resultMapper.delete(new LambdaQueryWrapper<CompetitionFormalCandidateResultEntity>()
        .eq(CompetitionFormalCandidateResultEntity::getCandidateExamId, candidateExamId));
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
