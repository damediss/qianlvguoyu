package com.guoyu.platform.competition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CompetitionTheoryAnswerSnapshotAdapter {

  private final ObjectMapper objectMapper;

  public CompetitionTheoryAnswerSnapshotAdapter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ParsedAnswer parse(String rawType, String content, String answerJson) {
    String type = normalizeType(rawType);
    if (!StringUtils.hasText(content)) {
      throw invalid("题干不能为空");
    }
    JsonNode root;
    try {
      root = objectMapper.readTree(answerJson);
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw invalid("题目答案不是有效JSON数组");
    }
    if (root == null || !root.isArray() || root.isEmpty()) {
      throw invalid("题目答案必须是非空JSON数组");
    }
    ArrayNode options = objectMapper.createArrayNode();
    ArrayNode correctAnswers = objectMapper.createArrayNode();
    Set<String> contents = new HashSet<>();
    Set<String> keys = new HashSet<>();
    int index = 0;
    for (JsonNode option : root) {
      index++;
      if (!option.isObject() || !option.has("content") || !option.has("isCorrect")) {
        throw invalid("每个选项必须包含content和isCorrect");
      }
      String optionContent = option.path("content").asText(null);
      if (!StringUtils.hasText(optionContent)) {
        throw invalid("选项内容不能为空");
      }
      String normalizedContent = optionContent.trim();
      if (!contents.add(normalizedContent)) {
        throw invalid("同一题目不能包含重复选项");
      }
      String key = stableOptionKey(normalizedContent, index);
      if (!keys.add(key)) {
        throw invalid("选项快照键冲突");
      }
      ObjectNode projected = objectMapper.createObjectNode();
      projected.put("key", key);
      projected.put("content", optionContent);
      options.add(projected);
      if (asBoolean(option.get("isCorrect"))) {
        correctAnswers.add(key);
      }
    }
    int correctCount = correctAnswers.size();
    if ("single".equals(type) && correctCount != 1) {
      throw invalid("单选题必须恰好有一个正确选项");
    }
    if ("multi".equals(type) && correctCount < 2) {
      throw invalid("多选题必须至少有两个正确选项");
    }
    if ("judge".equals(type) && (options.size() != 2 || correctCount != 1)) {
      throw invalid("判断题必须有两个选项且恰好一个正确选项");
    }
    try {
      return new ParsedAnswer(
          type,
          objectMapper.writeValueAsString(options),
          objectMapper.writeValueAsString(correctAnswers));
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化题目答案快照失败");
    }
  }

  public String normalizeType(String rawType) {
    String type = rawType == null ? "" : rawType.trim().toLowerCase(Locale.ROOT);
    if ("multiple".equals(type)) {
      return "multi";
    }
    if (!"single".equals(type) && !"multi".equals(type) && !"judge".equals(type)) {
      throw invalid("竞赛理论考试只允许单选、多选和判断题");
    }
    return type;
  }

  private boolean asBoolean(JsonNode value) {
    if (value == null || value.isNull()) {
      throw invalid("isCorrect不能为空");
    }
    if (value.isBoolean()) {
      return value.booleanValue();
    }
    if (value.isInt() || value.isLong()) {
      int number = value.asInt();
      if (number == 0 || number == 1) {
        return number == 1;
      }
    }
    if (value.isTextual()) {
      String text = value.asText().trim().toLowerCase(Locale.ROOT);
      if ("true".equals(text) || "1".equals(text)) {
        return true;
      }
      if ("false".equals(text) || "0".equals(text)) {
        return false;
      }
    }
    throw invalid("isCorrect必须是布尔值");
  }

  private String stableOptionKey(String content, int index) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((content + "\u0000" + index).getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder("OPT_");
      for (int i = 0; i < 8; i++) {
        result.append(String.format("%02x", hash[i]));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  private BusinessException invalid(String message) {
    return new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, message);
  }

  public record ParsedAnswer(
      String normalizedType,
      String optionsJson,
      String correctAnswersJson
  ) {
  }
}
