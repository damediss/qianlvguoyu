package com.guoyu.platform.competition.exam.formal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Single conversion/validation layer for candidate-facing option IDs and answer payloads.
 * Display labels are not persisted as the answer identity.
 */
@Component
public class FormalExamCandidateQuestionProjection {

  private static final Set<String> CHOICE_TYPES = Set.of("single", "multi", "judge");
  private final ObjectMapper objectMapper;

  public FormalExamCandidateQuestionProjection(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<ProjectedOption> projectOptions(Long questionSnapshotId, String optionsJson) {
    if (questionSnapshotId == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "正式题目缺少快照标识");
    }
    JsonNode root = readTree(optionsJson);
    List<ProjectedOption> options = new ArrayList<>();
    if (root == null || root.isNull()) {
      return options;
    }
    if (root.isObject()) {
      root.fields().forEachRemaining(entry -> {
        String key = normalizeKey(entry.getKey());
        if (!StringUtils.hasText(key)) {
          return;
        }
        options.add(new ProjectedOption(
            optionId(questionSnapshotId, key),
            displayLabel(key, options.size()),
            text(entry.getValue())));
      });
      return options;
    }
    if (!root.isArray()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "正式题目选项格式异常");
    }
    int index = 0;
    for (JsonNode node : root) {
      if (node == null || node.isNull()) {
        index++;
        continue;
      }
      String key = optionKey(node, index);
      options.add(new ProjectedOption(
          optionId(questionSnapshotId, key),
          displayLabel(existingKey(node), index),
          optionContent(node)));
      index++;
    }
    return options;
  }

  public String optionId(Long questionSnapshotId, String snapshotKey) {
    return questionSnapshotId + ":" + snapshotKey;
  }

  public NormalizedAnswer normalizeAnswer(
      String questionType, JsonNode answer, List<ProjectedOption> options) {
    String type = questionType == null ? "" : questionType.trim().toLowerCase(Locale.ROOT);
    if ("fill".equals(type)) {
      return normalizeFill(answer);
    }
    if (!CHOICE_TYPES.contains(type)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前题型不支持作答");
    }
    return normalizeChoice(type, answer, options);
  }

  public boolean samePayload(NormalizedAnswer left, NormalizedAnswer right, boolean leftMarked, boolean rightMarked) {
    if (leftMarked != rightMarked) {
      return false;
    }
    if (left == null || right == null) {
      return left == null && right == null;
    }
    return left.json().equals(right.json()) && left.answered() == right.answered();
  }

  public NormalizedAnswer parseStored(String questionType, String answerJson, List<ProjectedOption> options) {
    if (!StringUtils.hasText(answerJson)) {
      return normalizeAnswer(questionType, objectMapper.createObjectNode(), options);
    }
    return normalizeAnswer(questionType, readTree(answerJson), options);
  }

  private NormalizedAnswer normalizeChoice(
      String type, JsonNode answer, List<ProjectedOption> options) {
    Set<String> allowed = new LinkedHashSet<>();
    for (ProjectedOption option : options) {
      allowed.add(option.optionId());
    }
    LinkedHashSet<String> selected = new LinkedHashSet<>();
    JsonNode ids = answer == null ? null : answer.get("selectedOptionIds");
    if (ids != null && !ids.isNull() && !ids.isArray()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "选项答案格式无效");
    }
    if (ids != null && ids.isArray()) {
      for (JsonNode idNode : ids) {
        if (idNode == null || idNode.isNull()) {
          continue;
        }
        String optionId = idNode.asText("").trim();
        if (!StringUtils.hasText(optionId)) {
          continue;
        }
        if (!allowed.contains(optionId)) {
          throw new BusinessException(ErrorCode.FORBIDDEN, "选项不属于当前题目");
        }
        if (!selected.add(optionId)) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "选项答案不能重复");
        }
      }
    }
    if ("single".equals(type) || "judge".equals(type)) {
      if (selected.size() > 1) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "本题只能选择一个选项");
      }
    }
    ObjectNode payload = objectMapper.createObjectNode();
    ArrayNode array = payload.putArray("selectedOptionIds");
    selected.forEach(array::add);
    return new NormalizedAnswer(write(payload), !selected.isEmpty(), payload);
  }

  private NormalizedAnswer normalizeFill(JsonNode answer) {
    List<String> blanks = new ArrayList<>();
    JsonNode node = answer == null ? null : answer.get("blanks");
    if (node != null && !node.isNull() && !node.isArray()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "填空答案格式无效");
    }
    if (node != null && node.isArray()) {
      for (JsonNode blank : node) {
        blanks.add(blank == null || blank.isNull() ? "" : blank.asText(""));
      }
    }
    boolean answered = blanks.stream().anyMatch(StringUtils::hasText);
    if (!answered) {
      blanks = List.of();
    }
    ObjectNode payload = objectMapper.createObjectNode();
    ArrayNode array = payload.putArray("blanks");
    blanks.forEach(array::add);
    return new NormalizedAnswer(write(payload), answered, payload);
  }

  private JsonNode readTree(String json) {
    if (!StringUtils.hasText(json)) {
      return objectMapper.createArrayNode();
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "正式题目选项格式异常");
    }
  }

  private String write(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(node);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答案序列化失败");
    }
  }

  private static String optionKey(JsonNode node, int index) {
    String existing = existingKey(node);
    if (StringUtils.hasText(existing)) {
      return normalizeKey(existing);
    }
    return "i" + index;
  }

  private static String existingKey(JsonNode node) {
    if (node == null || !node.isObject()) {
      return null;
    }
    for (String field : List.of("key", "optionKey", "optionId", "id")) {
      String value = node.path(field).asText(null);
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private static String optionContent(JsonNode node) {
    if (node == null || node.isNull()) {
      return "";
    }
    if (node.isTextual()) {
      return node.asText("");
    }
    if (node.hasNonNull("content")) {
      return text(node.get("content"));
    }
    if (node.hasNonNull("text")) {
      return text(node.get("text"));
    }
    return "";
  }

  private static String text(JsonNode node) {
    return node == null || node.isNull() ? "" : node.asText("");
  }

  private static String normalizeKey(String key) {
    return key == null ? null : key.trim();
  }

  private static String displayLabel(String key, int index) {
    if (StringUtils.hasText(key) && key.matches("[A-Ha-h]")) {
      return key.toUpperCase(Locale.ROOT);
    }
    return String.valueOf((char) ('A' + Math.min(index, 25)));
  }

  public record ProjectedOption(String optionId, String label, String content) {
  }

  public record NormalizedAnswer(String json, boolean answered, JsonNode node) {
  }
}
