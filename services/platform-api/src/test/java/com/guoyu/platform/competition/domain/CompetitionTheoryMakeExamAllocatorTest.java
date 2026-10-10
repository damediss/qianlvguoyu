package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.competition.domain.CompetitionTheoryMakeExamAllocator.CandidateInput;
import com.guoyu.platform.competition.domain.CompetitionTheoryMakeExamAllocator.RuleInput;
import com.guoyu.platform.competition.domain.CompetitionTheoryMakeExamAllocator.TopicInput;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionTheoryMakeExamAllocatorTest {

  private final CompetitionTheoryMakeExamAllocator allocator = new CompetitionTheoryMakeExamAllocator();

  @Test
  void allocatesLevelThreeQuotasWithLargestRemainder() {
    var result = allocator.allocate(
        List.of(rule(0, "single", 20, 1)),
        topics("0.50", "0.30", "0.20"),
        List.of(),
        7L);

    assertThat(result.totalQuestions()).isEqualTo(20);
    assertThat(result.topics()).extracting(item -> item.questionCount()).containsExactly(10, 6, 4);
    assertThat(result.topics()).allSatisfy(item -> assertThat(item.shortage()).isEqualTo(item.questionCount()));
  }

  @Test
  void keepsEveryRuleRowIndependentWhenTypesRepeat() {
    var result = allocator.allocate(
        List.of(rule(0, "single", 10, 1), rule(1, "judge", 10, 1)),
        topics("0.50", "0.30", "0.20"),
        candidatesForTypes("single", "judge"),
        11L);

    assertThat(result.details()).extracting(item -> item.ruleIndex()).containsExactly(0, 1);
    assertThat(result.details()).extracting(item -> item.required()).containsExactly(10, 10);
    assertThat(result.topics()).flatExtracting(item -> item.details())
        .extracting(item -> item.required())
        .containsExactly(5, 5, 3, 3, 2, 2);
    assertThat(result.topics()).extracting(item -> item.questionCount()).containsExactly(10, 6, 4);
    assertThat(result.topics()).flatExtracting(item -> item.details())
        .extracting(item -> item.required()).containsExactly(5, 5, 3, 3, 2, 2);
  }

  @Test
  void allocatesSevenQuestionsAsFourTwoOne() {
    var result = allocator.allocate(
        List.of(rule(0, "single", 7, 1)),
        topics("0.50", "0.30", "0.20"),
        List.of(),
        13L);

    assertThat(result.topics()).extracting(item -> item.questionCount()).containsExactly(4, 2, 1);
  }

  @Test
  void doesNotForceZeroWeightOrTinyPositiveWeightTopicsToReceiveAQuestion() {
    var zeroResult = allocator.allocate(
        List.of(rule(0, "single", 10, 1)),
        topics("1", "0"),
        List.of(),
        17L);
    assertThat(zeroResult.topics()).extracting(item -> item.questionCount()).containsExactly(10, 0);
    assertThat(zeroResult.warnings()).isEmpty();

    var tinyResult = allocator.allocate(
        List.of(rule(0, "single", 1, 1)),
        topics("0.99", "0.01"),
        List.of(),
        19L);
    assertThat(tinyResult.topics()).extracting(item -> item.questionCount()).containsExactly(1, 0);
    assertThat(tinyResult.warnings()).anyMatch(item -> item.contains("T-2") && item.contains("0题"));
  }

  @Test
  void rejectsMissingOrInvalidWeightsInsteadOfFallingBackToEqualWeights() {
    assertThatThrownBy(() -> allocator.allocate(
        List.of(rule(0, "single", 1, 1)),
        List.of(new TopicInput(1L, 3, "T-1", "A-1", null, 1)),
        List.of(),
        23L)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("缺少明确权重");

    assertThatThrownBy(() -> allocator.allocate(
        List.of(rule(0, "single", 1, 1)),
        topics("0.60", "0.20"),
        List.of(),
        23L)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("不等于100%");

    assertThatThrownBy(() -> allocator.allocate(
        List.of(rule(0, "single", 1, 1)),
        List.of(new TopicInput(1L, 4, "T-1", "A-1", new BigDecimal("0.80"), 1),
            new TopicInput(2L, 3, "T-2", "A-2", new BigDecimal("0.20"), 2)),
        List.of(new CandidateInput(1L, 1L, "single", 2)),
        23L)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nodeLevel=3");
  }

  @Test
  void preservesMatrixAndSelectionForTheSameInputAndSeed() {
    List<RuleInput> rules = List.of(rule(0, "single", 4, 2), rule(1, "judge", 3, 1));
    List<TopicInput> topics = topics("0.50", "0.30", "0.20");
    List<CandidateInput> candidates = candidatesForTypes("single", "judge");

    var first = allocator.allocate(rules, topics, candidates, 29L);
    var second = allocator.allocate(rules, topics, candidates, 29L);

    assertThat(second).isEqualTo(first);
    assertThat(first.topics().stream().mapToInt(item -> item.questionCount()).sum()).isEqualTo(7);
    assertThat(first.details().stream().mapToInt(item -> item.required()).sum()).isEqualTo(7);
  }

  @Test
  void usesMaximumMatchingForOverlappingDifficultyRules() {
    var result = allocator.allocate(
        List.of(
            new RuleInput(0, "single", 1, BigDecimal.ONE, 2, 3, 1),
            new RuleInput(1, "single", 1, BigDecimal.ONE, 3, 3, 2)),
        List.of(new TopicInput(1L, 3, "设备知识", "A-1-1", BigDecimal.ONE, 1)),
        List.of(new CandidateInput(100L, 1L, "single", 3), new CandidateInput(101L, 1L, "single", 2)),
        31L);

    assertThat(result.ready()).isTrue();
    assertThat(result.selections()).hasSize(2);
    assertThat(result.selections()).extracting(item -> item.ruleIndex()).containsExactly(0, 1);
    assertThat(result.selections()).extracting(item -> item.questionId()).doesNotHaveDuplicates();
  }

  @Test
  void reportsExactShortageForAThreeDimensionalCell() {
    var result = allocator.allocate(
        List.of(new RuleInput(0, "judge", 3, BigDecimal.ONE, 2, 3, 1)),
        List.of(new TopicInput(1L, 3, "设备知识", "A-1-1", BigDecimal.ONE, 1)),
        List.of(new CandidateInput(200L, 1L, "judge", 2)),
        37L);

    assertThat(result.ready()).isFalse();
    assertThat(result.errors()).anyMatch(item -> item.contains("设备知识 / 判断题 / 难度2-3")
        && item.contains("需要3题") && item.contains("可用1题") && item.contains("缺少2题"));
    assertThat(result.topics().get(0).details().get(0).shortage()).isEqualTo(2);
  }

  @Test
  void deduplicatesQuestionIdsBeforeMatching() {
    var result = allocator.allocate(
        List.of(rule(0, "single", 2, 1)),
        List.of(new TopicInput(1L, 3, "T-1", "A-1-1", BigDecimal.ONE, 1)),
        List.of(new CandidateInput(300L, 1L, "single", 2), new CandidateInput(300L, 1L, "single", 2),
            new CandidateInput(301L, 1L, "single", 2)),
        41L);

    assertThat(result.ready()).isTrue();
    assertThat(result.selections()).extracting(item -> item.questionId()).doesNotHaveDuplicates();
    assertThat(result.selections()).extracting(item -> item.questionId()).containsExactlyInAnyOrder(300L, 301L);
  }

  private RuleInput rule(int index, String type, int count, int point) {
    return new RuleInput(index, type, count, BigDecimal.valueOf(point), null, null, index + 1);
  }

  private List<TopicInput> topics(String... weights) {
    List<TopicInput> result = new ArrayList<>();
    for (int index = 0; index < weights.length; index++) {
      result.add(new TopicInput(index + 1L, 3, "T-" + (index + 1), "A-" + (index + 1),
          new BigDecimal(weights[index]), index + 1));
    }
    return result;
  }

  private List<CandidateInput> candidatesForTypes(String... types) {
    List<CandidateInput> result = new ArrayList<>();
    long id = 1L;
    for (String type : types) {
      for (long topicId = 1L; topicId <= 3L; topicId++) {
        for (int index = 0; index < 10; index++) {
          result.add(new CandidateInput(id++, topicId, type, 2));
        }
      }
    }
    return result;
  }
}
