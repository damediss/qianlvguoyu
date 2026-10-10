package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class CompetitionTheoryAnswerSnapshotAdapterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final CompetitionTheoryAnswerSnapshotAdapter adapter =
      new CompetitionTheoryAnswerSnapshotAdapter(objectMapper);

  @Test
  void normalizesHistoricalMultipleAndCreatesStableKeys() throws Exception {
    String answer = """
        [
          {"content":"选项甲","isCorrect":true},
          {"content":"选项乙","isCorrect":"1"},
          {"content":"选项丙","isCorrect":false}
        ]
        """;

    CompetitionTheoryAnswerSnapshotAdapter.ParsedAnswer first =
        adapter.parse("multiple", "多选题干", answer);
    CompetitionTheoryAnswerSnapshotAdapter.ParsedAnswer second =
        adapter.parse("multi", "多选题干", answer);

    assertThat(first.normalizedType()).isEqualTo("multi");
    assertThat(first.optionsJson()).isEqualTo(second.optionsJson());
    assertThat(first.correctAnswersJson()).isEqualTo(second.correctAnswersJson());
    JsonNode options = objectMapper.readTree(first.optionsJson());
    JsonNode correct = objectMapper.readTree(first.correctAnswersJson());
    assertThat(options).hasSize(3);
    assertThat(options.get(0).path("key").asText()).startsWith("OPT_");
    assertThat(options.get(0).has("isCorrect")).isFalse();
    assertThat(correct).hasSize(2);
  }

  @Test
  void acceptsSingleAndJudgeWithExactlyOneCorrectOption() {
    String answer = """
        [
          {"content":"正确","isCorrect":1},
          {"content":"错误","isCorrect":0}
        ]
        """;

    assertThat(adapter.parse("single", "单选题干", answer).normalizedType())
        .isEqualTo("single");
    assertThat(adapter.parse("judge", "判断题干", answer).normalizedType())
        .isEqualTo("judge");
  }

  @Test
  void rejectsSubjectiveMalformedAndAmbiguousAnswers() {
    assertThatThrownBy(() -> adapter.parse("essay", "题干", "[]"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("竞赛理论考试只允许单选、多选和判断题");
    assertThatThrownBy(() -> adapter.parse("single", "题干", "not-json"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("题目答案不是有效JSON数组");
    assertThatThrownBy(() -> adapter.parse("single", "题干", """
        [
          {"content":"甲","isCorrect":true},
          {"content":"乙","isCorrect":true}
        ]
        """))
        .isInstanceOf(BusinessException.class)
        .hasMessage("单选题必须恰好有一个正确选项");
    assertThatThrownBy(() -> adapter.parse("multi", "题干", """
        [
          {"content":"甲","isCorrect":true},
          {"content":"乙","isCorrect":false}
        ]
        """))
        .isInstanceOf(BusinessException.class)
        .hasMessage("多选题必须至少有两个正确选项");
    assertThatThrownBy(() -> adapter.parse("judge", "题干", """
        [
          {"content":"正确","isCorrect":true},
          {"content":"错误","isCorrect":false},
          {"content":"未知","isCorrect":false}
        ]
        """))
        .isInstanceOf(BusinessException.class)
        .hasMessage("判断题必须有两个选项且恰好一个正确选项");
  }
}
