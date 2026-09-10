package com.guoyu.platform.competition.exam.formal.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormalExamQuestionScorerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final FormalExamQuestionScorerRegistry registry = new FormalExamQuestionScorerRegistry(
      List.of(
          new FormalExamSingleChoiceScorer(support()),
          new FormalExamMultipleChoiceScorer(support()),
          new FormalExamJudgeScorer(support()),
          new FormalExamFillScorer(support())));

  private FormalExamChoiceScoringSupport support() {
    return new FormalExamChoiceScoringSupport(
        new FormalExamCandidateQuestionProjection(objectMapper), objectMapper);
  }

  @Test
  void singleChoiceAwardsFullScoreOnlyForExactOption() {
    CompetitionFormalExamQuestionSnapshotEntity question = choice(9301L, "single", "B", "1.50");
    assertThat(registry.score(question, choiceAnswer(9301L, 4, "9301:B")).awardedScore())
        .isEqualByComparingTo("1.50");
    assertThat(registry.score(question, choiceAnswer(9301L, 4, "9301:B")).correct()).isTrue();
    assertThat(registry.score(question, choiceAnswer(9301L, 3, "9301:A")).awardedScore())
        .isEqualByComparingTo("0.00");
    FormalExamQuestionScoreOutcome unanswered = registry.score(question, null);
    assertThat(unanswered.answered()).isFalse();
    assertThat(unanswered.correct()).isFalse();
    assertThat(unanswered.answerVersion()).isEqualTo(0);
    assertThat(unanswered.awardedScore()).isEqualByComparingTo("0.00");
  }

  @Test
  void judgeUsesTheSameOptionSnapshotAsChoice() {
    CompetitionFormalExamQuestionSnapshotEntity question = choice(9401L, "judge", "A", "1");
    assertThat(registry.score(question, choiceAnswer(9401L, 1, "9401:A")).correct()).isTrue();
    assertThat(registry.score(question, choiceAnswer(9401L, 1, "9401:B")).correct()).isFalse();
    assertThat(registry.score(question, null).answered()).isFalse();
  }

  @Test
  void multipleChoiceRequiresExactSetRegardlessOfOrder() {
    CompetitionFormalExamQuestionSnapshotEntity question =
        choice(9501L, "multi", "[\"A\",\"C\"]", "2.00");
    assertThat(registry.score(question, choiceAnswer(9501L, 2, "9501:A", "9501:C")).awardedScore())
        .isEqualByComparingTo("2.00");
    assertThat(registry.score(question, choiceAnswer(9501L, 2, "9501:C", "9501:A")).awardedScore())
        .isEqualByComparingTo("2.00");
    assertThat(registry.score(question, choiceAnswer(9501L, 2, "9501:A")).awardedScore())
        .isEqualByComparingTo("0.00");
    assertThat(registry.score(question, choiceAnswer(9501L, 2, "9501:A", "9501:B", "9501:C"))
        .awardedScore()).isEqualByComparingTo("0.00");
  }

  @Test
  void fillTrimsExactMatchAndDoesNotIgnoreCase() {
    CompetitionFormalExamQuestionSnapshotEntity question = fill(9601L, "答案", "1");
    assertThat(registry.score(question, fillAnswer(9601L, 1, "答案")).correct()).isTrue();
    assertThat(registry.score(question, fillAnswer(9601L, 1, " 答案 ")).correct()).isTrue();
    assertThat(registry.score(question, fillAnswer(9601L, 1, "答案 ")).awardedScore())
        .isEqualByComparingTo("1.00");
    assertThat(registry.score(question, fillAnswer(9601L, 1, "错误")).awardedScore())
        .isEqualByComparingTo("0.00");
    assertThat(registry.score(question, fillAnswer(9601L, 1, "答案x")).correct()).isFalse();
    assertThat(registry.score(question, null).answered()).isFalse();
  }

  @Test
  void fillRejectsMultiBlankPayload() {
    CompetitionFormalExamQuestionSnapshotEntity question = fill(9602L, "答案", "1");
    CompetitionFormalCandidateAnswerEntity answer = new CompetitionFormalCandidateAnswerEntity();
    answer.setQuestionSnapshotId(9602L);
    answer.setAnswerVersion(1);
    answer.setAnswerJson("{\"blanks\":[\"答案\",\"另一空\"]}");
    assertThatThrownBy(() -> registry.score(question, answer))
        .isInstanceOf(FormalExamScoringException.class)
        .hasMessageContaining("多空");
  }

  @Test
  void malformedCorrectAnswersFailInsteadOfZero() {
    CompetitionFormalExamQuestionSnapshotEntity question = choice(9301L, "single", "not-json", "1");
    assertThatThrownBy(() -> registry.score(question, choiceAnswer(9301L, 1, "9301:A")))
        .isInstanceOf(FormalExamScoringException.class);
  }

  @Test
  void illegalOptionFailsInsteadOfZero() {
    CompetitionFormalExamQuestionSnapshotEntity question = choice(9301L, "single", "A", "1");
    assertThatThrownBy(() -> registry.score(question, choiceAnswer(9301L, 1, "9301:Z")))
        .isInstanceOf(FormalExamScoringException.class);
  }

  @Test
  void unknownTypeFails() {
    CompetitionFormalExamQuestionSnapshotEntity question = choice(9301L, "essay", "A", "1");
    assertThatThrownBy(() -> registry.score(question, null))
        .isInstanceOf(FormalExamScoringException.class)
        .hasMessageContaining("未知题型");
  }

  private CompetitionFormalExamQuestionSnapshotEntity choice(
      Long id, String type, String correct, String score) {
    CompetitionFormalExamQuestionSnapshotEntity question =
        new CompetitionFormalExamQuestionSnapshotEntity();
    question.setId(id);
    question.setExamSessionId(9001L);
    question.setQuestionOrder(1);
    question.setQuestionType(type);
    question.setScore(new BigDecimal(score));
    question.setOptionsJson("[{\"key\":\"A\",\"content\":\"甲\"},{\"key\":\"B\",\"content\":\"乙\"},"
        + "{\"key\":\"C\",\"content\":\"丙\"}]");
    question.setCorrectAnswersJson(correct.startsWith("[") ? correct : "[\"" + correct + "\"]");
    return question;
  }

  private CompetitionFormalExamQuestionSnapshotEntity fill(Long id, String fillAnswer, String score) {
    CompetitionFormalExamQuestionSnapshotEntity question =
        new CompetitionFormalExamQuestionSnapshotEntity();
    question.setId(id);
    question.setExamSessionId(9001L);
    question.setQuestionOrder(1);
    question.setQuestionType("fill");
    question.setScore(new BigDecimal(score));
    question.setOptionsJson("[]");
    question.setFillAnswer(fillAnswer);
    return question;
  }

  private CompetitionFormalCandidateAnswerEntity choiceAnswer(
      Long questionId, int version, String... optionIds) {
    CompetitionFormalCandidateAnswerEntity answer = new CompetitionFormalCandidateAnswerEntity();
    answer.setQuestionSnapshotId(questionId);
    answer.setAnswerVersion(version);
    String joined = String.join("\",\"", optionIds);
    answer.setAnswerJson("{\"selectedOptionIds\":[\"" + joined + "\"]}");
    answer.setIsAnswered(1);
    return answer;
  }

  private CompetitionFormalCandidateAnswerEntity fillAnswer(Long questionId, int version, String blank) {
    CompetitionFormalCandidateAnswerEntity answer = new CompetitionFormalCandidateAnswerEntity();
    answer.setQuestionSnapshotId(questionId);
    answer.setAnswerVersion(version);
    answer.setAnswerJson("{\"blanks\":[\"" + blank + "\"]}");
    answer.setIsAnswered(1);
    return answer;
  }
}
