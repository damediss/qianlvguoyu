package com.guoyu.platform.competition.exam.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CompetitionExamVoSerializationTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void playerQuestionProjectionContainsNoAnswerResolutionSourceOrPaperCode() {
    ArrayNode options = objectMapper.createArrayNode();
    options.addObject().put("key", "OPT_a").put("content", "选项A");
    CompetitionExamQuestionVO vo = new CompetitionExamQuestionVO(
        1L, 1, "single", "题干", options, BigDecimal.ONE);

    var json = objectMapper.valueToTree(vo);

    assertThat(json.has("correctAnswers")).isFalse();
    assertThat(json.has("resolveContent")).isFalse();
    assertThat(json.has("sourceOrgName")).isFalse();
    assertThat(json.has("paperCode")).isFalse();
  }
}
