package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateQuestionScoreEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateResultEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamChoiceScoringSupport;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamFillScorer;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamJudgeScorer;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamMultipleChoiceScorer;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamQuestionScorerRegistry;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamScoringException;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamSingleChoiceScorer;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateQuestionScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateResultMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompetitionFormalExamCandidateScoringServiceTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 11, 0, 0);

  private final CompetitionFormalCandidateExamMapper candidates =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionFormalExamPaperSnapshotMapper papers =
      Mockito.mock(CompetitionFormalExamPaperSnapshotMapper.class);
  private final CompetitionFormalExamQuestionSnapshotMapper questions =
      Mockito.mock(CompetitionFormalExamQuestionSnapshotMapper.class);
  private final CompetitionFormalCandidateAnswerMapper answers =
      Mockito.mock(CompetitionFormalCandidateAnswerMapper.class);
  private final CompetitionFormalCandidateResultMapper results =
      Mockito.mock(CompetitionFormalCandidateResultMapper.class);
  private final CompetitionFormalCandidateQuestionScoreMapper scores =
      Mockito.mock(CompetitionFormalCandidateQuestionScoreMapper.class);

  private CompetitionFormalExamCandidateScoringService service;

  @BeforeEach
  void setup() {
    ObjectMapper objectMapper = new ObjectMapper();
    FormalExamChoiceScoringSupport support = new FormalExamChoiceScoringSupport(
        new FormalExamCandidateQuestionProjection(objectMapper), objectMapper);
    service = new CompetitionFormalExamCandidateScoringService(
        candidates, papers, questions, answers, results, scores,
        new FormalExamQuestionScorerRegistry(List.of(
            new FormalExamSingleChoiceScorer(support),
            new FormalExamMultipleChoiceScorer(support),
            new FormalExamJudgeScorer(support),
            new FormalExamFillScorer(support))));
    when(results.insert(any(CompetitionFormalCandidateResultEntity.class))).thenReturn(1);
    when(results.updateById(any(CompetitionFormalCandidateResultEntity.class))).thenReturn(1);
    when(scores.insert(any(CompetitionFormalCandidateQuestionScoreEntity.class))).thenReturn(1);
    when(results.delete(any())).thenReturn(1);
    when(scores.delete(any())).thenReturn(1);
  }

  @Test
  void submittedCandidateProducesFullQuestionScoresAndTotal() {
    stubSubmitted("submitted");
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(
        question(9301L, 1, "single", "1.00", "[\"B\"]", null),
        question(9302L, 2, "multi", "2.00", "[\"A\",\"C\"]", null),
        question(9303L, 3, "fill", "1.00", null, "答案")));
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of(
        choice(9301L, 4, "9301:B"),
        choice(9302L, 5, "9302:C", "9302:A")));

    service.scoreCandidate(9001L, 9101L, NOW);

    ArgumentCaptor<CompetitionFormalCandidateQuestionScoreEntity> scoreCaptor =
        ArgumentCaptor.forClass(CompetitionFormalCandidateQuestionScoreEntity.class);
    verify(scores, times(3)).insert(scoreCaptor.capture());
    assertThat(scoreCaptor.getAllValues()).hasSize(3);
    assertThat(scoreCaptor.getAllValues().get(0).getAwardedScore()).isEqualByComparingTo("1.00");
    assertThat(scoreCaptor.getAllValues().get(0).getAnswerVersion()).isEqualTo(4);
    assertThat(scoreCaptor.getAllValues().get(1).getAwardedScore()).isEqualByComparingTo("2.00");
    assertThat(scoreCaptor.getAllValues().get(2).getIsAnswered()).isEqualTo(0);
    assertThat(scoreCaptor.getAllValues().get(2).getAnswerVersion()).isEqualTo(0);
    ArgumentCaptor<CompetitionFormalCandidateResultEntity> resultCaptor =
        ArgumentCaptor.forClass(CompetitionFormalCandidateResultEntity.class);
    verify(results).updateById(resultCaptor.capture());
    assertThat(resultCaptor.getValue().getResultStatus()).isEqualTo("scored");
    assertThat(resultCaptor.getValue().getTotalScore()).isEqualByComparingTo("3.00");
    assertThat(resultCaptor.getValue().getFullScore()).isEqualByComparingTo("4.00");
    assertThat(resultCaptor.getValue().getAnsweredCount()).isEqualTo(2);
    assertThat(resultCaptor.getValue().getCorrectCount()).isEqualTo(2);
    assertThat(resultCaptor.getValue().getWrongCount()).isEqualTo(0);
    assertThat(resultCaptor.getValue().getUnansweredCount()).isEqualTo(1);
  }

  @Test
  void autoAndForcedUseTheSameRulesAsSubmitted() {
    for (String status : List.of("auto_submitted", "forced_submitted")) {
      Mockito.reset(results, scores, papers, questions, answers, candidates);
      when(results.insert(any(CompetitionFormalCandidateResultEntity.class))).thenReturn(1);
      when(results.updateById(any(CompetitionFormalCandidateResultEntity.class))).thenReturn(1);
      when(scores.insert(any(CompetitionFormalCandidateQuestionScoreEntity.class))).thenReturn(1);
      stubSubmitted(status);
      CompetitionFormalExamPaperSnapshotEntity paper = paper();
      paper.setQuestionCount(1);
      paper.setTotalScore(new BigDecimal("1.00"));
      when(papers.selectById(9201L)).thenReturn(paper);
      when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(
          question(9301L, 1, "single", "1.00", "[\"B\"]", null)));
      when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of(choice(9301L, 1, "9301:B")));
      service.scoreCandidate(9001L, 9101L, NOW);
      ArgumentCaptor<CompetitionFormalCandidateResultEntity> resultCaptor =
          ArgumentCaptor.forClass(CompetitionFormalCandidateResultEntity.class);
      verify(results).updateById(resultCaptor.capture());
      assertThat(resultCaptor.getValue().getTotalScore()).isEqualByComparingTo("1.00");
    }
  }

  @Test
  void absentCreatesNullScoreWithoutQuestionScores() {
    CompetitionFormalCandidateExamEntity candidate = candidate("absent");
    when(candidates.selectById(9101L)).thenReturn(candidate);
    when(results.selectByCandidateExamId(9101L)).thenReturn(null);

    assertThat(service.scoreCandidate(9001L, 9101L, NOW))
        .isEqualTo(CompetitionFormalExamCandidateScoringService.Outcome.ABSENT);

    ArgumentCaptor<CompetitionFormalCandidateResultEntity> resultCaptor =
        ArgumentCaptor.forClass(CompetitionFormalCandidateResultEntity.class);
    verify(results).insert(resultCaptor.capture());
    assertThat(resultCaptor.getValue().getResultStatus()).isEqualTo("absent");
    assertThat(resultCaptor.getValue().getTotalScore()).isNull();
    assertThat(resultCaptor.getValue().getFullScore()).isNull();
    assertThat(resultCaptor.getValue().getQuestionCount()).isNull();
    verify(scores, never()).insert(any(CompetitionFormalCandidateQuestionScoreEntity.class));
    verify(papers, never()).selectById(any());
  }

  @Test
  void completeResultIsSkippedOnRetry() {
    CompetitionFormalCandidateExamEntity candidate = candidate("submitted");
    when(candidates.selectById(9101L)).thenReturn(candidate);
    CompetitionFormalCandidateResultEntity result = new CompetitionFormalCandidateResultEntity();
    result.setResultStatus("scored");
    result.setScoreVersion(1);
    result.setTotalScore(new BigDecimal("1.00"));
    result.setFullScore(new BigDecimal("1.00"));
    result.setQuestionCount(1);
    when(results.selectByCandidateExamId(9101L)).thenReturn(result);
    CompetitionFormalCandidateQuestionScoreEntity score =
        new CompetitionFormalCandidateQuestionScoreEntity();
    score.setAwardedScore(new BigDecimal("1.00"));
    score.setMaxScore(new BigDecimal("1.00"));
    when(scores.selectByCandidateExamId(9101L)).thenReturn(List.of(score));
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(
        question(9301L, 1, "single", "1.00", "[\"B\"]", null)));

    assertThat(service.scoreCandidate(9001L, 9101L, NOW))
        .isEqualTo(CompetitionFormalExamCandidateScoringService.Outcome.SKIPPED);
    verify(results, never()).insert(any(CompetitionFormalCandidateResultEntity.class));
  }

  @Test
  void malformedSnapshotFailsCandidateInsteadOfZero() {
    stubSubmitted("forced_submitted");
    CompetitionFormalExamPaperSnapshotEntity paper = paper();
    paper.setQuestionCount(1);
    paper.setTotalScore(new BigDecimal("1.00"));
    when(papers.selectById(9201L)).thenReturn(paper);
    CompetitionFormalExamQuestionSnapshotEntity broken =
        question(9301L, 1, "single", "1.00", "not-json", null);
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(broken));
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of());

    assertThatThrownBy(() -> service.scoreCandidate(9001L, 9101L, NOW))
        .isInstanceOf(FormalExamScoringException.class);
  }

  @Test
  void oneHundredQuestionsProduceOneHundredScores() {
    stubSubmitted("submitted");
    List<CompetitionFormalExamQuestionSnapshotEntity> paperQuestions = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      paperQuestions.add(question(9300L + i, i, "single", "1.00", "[\"A\"]", null));
    }
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(paperQuestions);
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of());
    CompetitionFormalExamPaperSnapshotEntity paper = paper();
    paper.setQuestionCount(100);
    paper.setTotalScore(new BigDecimal("100.00"));
    when(papers.selectById(9201L)).thenReturn(paper);

    service.scoreCandidate(9001L, 9101L, NOW);

    verify(scores, times(100)).insert(any(CompetitionFormalCandidateQuestionScoreEntity.class));
    ArgumentCaptor<CompetitionFormalCandidateResultEntity> resultCaptor =
        ArgumentCaptor.forClass(CompetitionFormalCandidateResultEntity.class);
    verify(results).updateById(resultCaptor.capture());
    assertThat(resultCaptor.getValue().getQuestionCount()).isEqualTo(100);
    assertThat(resultCaptor.getValue().getUnansweredCount()).isEqualTo(100);
    assertThat(resultCaptor.getValue().getTotalScore()).isEqualByComparingTo("0.00");
  }

  private void stubSubmitted(String status) {
    when(candidates.selectById(9101L)).thenReturn(candidate(status));
    when(results.selectByCandidateExamId(9101L)).thenReturn(null);
    when(papers.selectById(9201L)).thenReturn(paper());
  }

  private CompetitionFormalCandidateExamEntity candidate(String status) {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setId(9101L);
    entity.setTenantId(1L);
    entity.setExamSessionId(9001L);
    entity.setPaperSnapshotId(9201L);
    entity.setStatus(status);
    entity.setAnsweredCount(0);
    return entity;
  }

  private CompetitionFormalExamPaperSnapshotEntity paper() {
    CompetitionFormalExamPaperSnapshotEntity paper = new CompetitionFormalExamPaperSnapshotEntity();
    paper.setId(9201L);
    paper.setExamSessionId(9001L);
    paper.setQuestionCount(3);
    paper.setTotalScore(new BigDecimal("4.00"));
    return paper;
  }

  private CompetitionFormalExamQuestionSnapshotEntity question(
      Long id, int order, String type, String score, String correct, String fill) {
    CompetitionFormalExamQuestionSnapshotEntity question =
        new CompetitionFormalExamQuestionSnapshotEntity();
    question.setId(id);
    question.setExamSessionId(9001L);
    question.setPaperSnapshotId(9201L);
    question.setQuestionOrder(order);
    question.setQuestionType(type);
    question.setScore(new BigDecimal(score));
    question.setOptionsJson("[{\"key\":\"A\",\"content\":\"甲\"},{\"key\":\"B\",\"content\":\"乙\"},"
        + "{\"key\":\"C\",\"content\":\"丙\"}]");
    question.setCorrectAnswersJson(correct);
    question.setFillAnswer(fill);
    return question;
  }

  private CompetitionFormalCandidateAnswerEntity choice(Long questionId, int version, String... ids) {
    CompetitionFormalCandidateAnswerEntity answer = new CompetitionFormalCandidateAnswerEntity();
    answer.setCandidateExamId(9101L);
    answer.setQuestionSnapshotId(questionId);
    answer.setAnswerVersion(version);
    answer.setAnswerJson("{\"selectedOptionIds\":[\"" + String.join("\",\"", ids) + "\"]}");
    answer.setIsAnswered(1);
    return answer;
  }
}
