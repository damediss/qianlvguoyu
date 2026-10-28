package com.guoyu.platform.exam.reviewsync.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.reviewsync.service.AnswerJudgeDomainService.AnswerOption;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/** Shared parser for the option array stored in exam_exam_detail.answer. */
@Slf4j
public final class AnswerOptionParser {

  private AnswerOptionParser() {
  }

  public static List<AnswerOption> parse(String answerJson, ObjectMapper objectMapper) {
    if (!StringUtils.hasText(answerJson) || objectMapper == null) {
      return List.of();
    }
    try {
      JsonNode root = objectMapper.readTree(answerJson);
      if (root == null || !root.isArray()) {
        return List.of();
      }
      List<AnswerOption> options = new ArrayList<>();
      for (JsonNode item : root) {
        if (item == null || !item.isObject()) {
          continue;
        }
        JsonNode contentNode = item.get("content");
        String content = contentNode == null || contentNode.isNull() ? "" : contentNode.asText("");
        JsonNode correctNode = item.get("isCorrect");
        boolean correct = correctNode != null
            && (correctNode.asBoolean(false)
                || (correctNode.isNumber() && correctNode.asInt() != 0)
                || Set.of("true", "1", "是", "正确", "对")
                    .contains(correctNode.asText().trim().toLowerCase(java.util.Locale.ROOT)));
        options.add(new AnswerOption(content, correct, parseScore(item.get("score"))));
      }
      return List.copyOf(options);
    } catch (Exception ex) {
      log.warn("Question option JSON could not be parsed; no options will be returned: {}",
          ex.getMessage());
      return List.of();
    }
  }

  private static BigDecimal parseScore(JsonNode scoreNode) {
    if (scoreNode == null || scoreNode.isNull()) {
      return null;
    }
    try {
      String value = scoreNode.asText().trim();
      return value.isEmpty() ? null : new BigDecimal(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
