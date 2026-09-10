package com.guoyu.platform.competition.exam.formal.scoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.NormalizedAnswer;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection.ProjectedOption;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FormalExamChoiceScoringSupport {

  private final FormalExamCandidateQuestionProjection projection;
  private final ObjectMapper objectMapper;

  public FormalExamChoiceScoringSupport(
      FormalExamCandidateQuestionProjection projection, ObjectMapper objectMapper) {
    this.projection = projection;
    this.objectMapper = objectMapper;
  }

  public FormalExamQuestionScoreOutcome scoreSet(
      CompetitionFormalExamQuestionSnapshotEntity question,
      CompetitionFormalCandidateAnswerEntity answer,
      int minCorrectSize,
      int maxCorrectSize) {
    BigDecimal maxScore = requireScore(question);
    List<ProjectedOption> options = projectOptions(question);
    if (options.isEmpty()) {
      throw scoringFailure("正式题目选项快照为空", question);
    }
    Set<String> allowedKeys = optionKeys(question.getId(), options);
    Set<String> correctKeys = parseCorrectKeys(question, allowedKeys);
    if (correctKeys.size() < minCorrectSize || correctKeys.size() > maxCorrectSize) {
      throw scoringFailure("正式题目标准答案数量与题型不匹配", question);
    }
    if (correctKeys.isEmpty()) {
      throw scoringFailure("正式题目标准答案为空", question);
    }
    if (answer == null) {
      return FormalExamQuestionScoreOutcome.unanswered(0, maxScore);
    }
    NormalizedAnswer stored = parseStored(question, answer, options);
    if (!stored.answered()) {
      return FormalExamQuestionScoreOutcome.unanswered(answerVersion(answer), maxScore);
    }
    Set<String> selectedKeys = selectedKeys(question.getId(), stored, allowedKeys);
    boolean correct = correctKeys.equals(selectedKeys);
    return FormalExamQuestionScoreOutcome.scored(
        correct, answerVersion(answer), maxScore, correct ? maxScore : FormalExamScoringDecimals.zero());
  }

  public FormalExamQuestionScoreOutcome scoreFill(
      CompetitionFormalExamQuestionSnapshotEntity question,
      CompetitionFormalCandidateAnswerEntity answer) {
    BigDecimal maxScore = requireScore(question);
    String standard = standardFill(question);
    if (answer == null) {
      return FormalExamQuestionScoreOutcome.unanswered(0, maxScore);
    }
    NormalizedAnswer stored;
    try {
      stored = projection.parseStored(question.getQuestionType(), answer.getAnswerJson(), List.of());
    } catch (BusinessException ex) {
      throw scoringFailure(ex.getMessage(), question, ex);
    }
    if (!stored.answered()) {
      return FormalExamQuestionScoreOutcome.unanswered(answerVersion(answer), maxScore);
    }
    List<String> blanks = blanks(stored);
    if (blanks.size() != 1) {
      throw scoringFailure("填空快照仅支持单空，当前作答存在多空结构", question);
    }
    boolean correct = standard.equals(blanks.getFirst().trim());
    return FormalExamQuestionScoreOutcome.scored(
        correct, answerVersion(answer), maxScore, correct ? maxScore : FormalExamScoringDecimals.zero());
  }

  private List<ProjectedOption> projectOptions(CompetitionFormalExamQuestionSnapshotEntity question) {
    try {
      return projection.projectOptions(question.getId(), question.getOptionsJson());
    } catch (BusinessException ex) {
      throw scoringFailure(ex.getMessage(), question, ex);
    }
  }

  private NormalizedAnswer parseStored(
      CompetitionFormalExamQuestionSnapshotEntity question,
      CompetitionFormalCandidateAnswerEntity answer,
      List<ProjectedOption> options) {
    try {
      return projection.parseStored(question.getQuestionType(), answer.getAnswerJson(), options);
    } catch (BusinessException ex) {
      throw scoringFailure(ex.getMessage(), question, ex);
    }
  }

  private Set<String> parseCorrectKeys(
      CompetitionFormalExamQuestionSnapshotEntity question, Set<String> allowedKeys) {
    JsonNode root = readJson(question.getCorrectAnswersJson(), "正式题目标准答案格式异常", question);
    if (root == null || !root.isArray() || root.isEmpty()) {
      throw scoringFailure("正式题目标准答案为空", question);
    }
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    for (JsonNode node : root) {
      String key = correctKey(node);
      if (!StringUtils.hasText(key)) {
        throw scoringFailure("正式题目标准答案格式异常", question);
      }
      if (!allowedKeys.contains(key)) {
        throw scoringFailure("正式题目标准答案不属于当前选项", question);
      }
      if (!keys.add(key)) {
        throw scoringFailure("正式题目标准答案存在重复选项", question);
      }
    }
    return keys;
  }

  private String correctKey(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual() || node.isNumber()) {
      return node.asText("").trim();
    }
    if (node.isObject()) {
      for (String field : List.of("key", "optionKey", "optionId", "id")) {
        String value = node.path(field).asText(null);
        if (StringUtils.hasText(value)) {
          return value.trim();
        }
      }
    }
    return null;
  }

  private Set<String> selectedKeys(
      Long questionSnapshotId, NormalizedAnswer stored, Set<String> allowedKeys) {
    JsonNode ids = stored.node() == null ? null : stored.node().get("selectedOptionIds");
    LinkedHashSet<String> selected = new LinkedHashSet<>();
    if (ids == null || !ids.isArray()) {
      throw new FormalExamScoringException("选手答案选项格式异常", null, null, questionSnapshotId);
    }
    for (JsonNode idNode : ids) {
      if (idNode == null || idNode.isNull()) {
        continue;
      }
      String optionId = idNode.asText("").trim();
      String key = snapshotKey(questionSnapshotId, optionId);
      if (!allowedKeys.contains(key)) {
        throw new FormalExamScoringException("选手答案包含非法选项", null, null, questionSnapshotId);
      }
      selected.add(key);
    }
    return selected;
  }

  private String snapshotKey(Long questionSnapshotId, String optionId) {
    String prefix = questionSnapshotId + ":";
    if (!StringUtils.hasText(optionId) || !optionId.startsWith(prefix)) {
      throw new FormalExamScoringException("选手答案选项不属于当前题目", null, null, questionSnapshotId);
    }
    String key = optionId.substring(prefix.length()).trim();
    if (!StringUtils.hasText(key)) {
      throw new FormalExamScoringException("选手答案选项格式异常", null, null, questionSnapshotId);
    }
    return key;
  }

  private Set<String> optionKeys(Long questionSnapshotId, List<ProjectedOption> options) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    for (ProjectedOption option : options) {
      keys.add(snapshotKey(questionSnapshotId, option.optionId()));
    }
    return keys;
  }

  private String standardFill(CompetitionFormalExamQuestionSnapshotEntity question) {
    if (StringUtils.hasText(question.getFillAnswer())) {
      String value = question.getFillAnswer().trim();
      if (looksLikeJsonArray(value)) {
        JsonNode root = readJson(value, "填空标准答案格式异常", question);
        return singleFillFromJson(root, question);
      }
      return value;
    }
    if (!StringUtils.hasText(question.getCorrectAnswersJson())) {
      throw scoringFailure("填空标准答案为空", question);
    }
    JsonNode root = readJson(question.getCorrectAnswersJson(), "填空标准答案格式异常", question);
    return singleFillFromJson(root, question);
  }

  private String singleFillFromJson(JsonNode root, CompetitionFormalExamQuestionSnapshotEntity question) {
    if (root != null && root.isTextual()) {
      String value = root.asText("").trim();
      if (!StringUtils.hasText(value)) {
        throw scoringFailure("填空标准答案为空", question);
      }
      return value;
    }
    if (root != null && root.isArray()) {
      if (root.size() > 1) {
        throw scoringFailure("填空快照仅支持单空，当前标准答案存在多空结构", question);
      }
      if (root.size() == 1 && (root.get(0).isTextual() || root.get(0).isNumber())) {
        String value = root.get(0).asText("").trim();
        if (!StringUtils.hasText(value)) {
          throw scoringFailure("填空标准答案为空", question);
        }
        return value;
      }
    }
    throw scoringFailure("填空标准答案格式异常", question);
  }

  private boolean looksLikeJsonArray(String value) {
    String trimmed = value.trim();
    return trimmed.startsWith("[") && trimmed.endsWith("]");
  }

  private List<String> blanks(NormalizedAnswer stored) {
    JsonNode node = stored.node() == null ? null : stored.node().get("blanks");
    if (node == null || !node.isArray()) {
      throw new FormalExamScoringException("填空答案格式异常");
    }
    java.util.ArrayList<String> blanks = new java.util.ArrayList<>();
    for (JsonNode blank : node) {
      blanks.add(blank == null || blank.isNull() ? "" : blank.asText(""));
    }
    return blanks;
  }

  private JsonNode readJson(
      String json, String message, CompetitionFormalExamQuestionSnapshotEntity question) {
    if (!StringUtils.hasText(json)) {
      return objectMapper.createArrayNode();
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      throw scoringFailure(message, question, ex);
    }
  }

  private BigDecimal requireScore(CompetitionFormalExamQuestionSnapshotEntity question) {
    if (question == null || question.getScore() == null
        || question.getScore().compareTo(BigDecimal.ZERO) < 0) {
      throw scoringFailure("正式题目分值无效", question);
    }
    return FormalExamScoringDecimals.money(question.getScore());
  }

  private int answerVersion(CompetitionFormalCandidateAnswerEntity answer) {
    if (answer.getAnswerVersion() == null || answer.getAnswerVersion() < 1) {
      throw new FormalExamScoringException(
          "选手答案版本无效", null, answer.getCandidateExamId(), answer.getQuestionSnapshotId());
    }
    return answer.getAnswerVersion();
  }

  private FormalExamScoringException scoringFailure(
      String message, CompetitionFormalExamQuestionSnapshotEntity question) {
    return scoringFailure(message, question, null);
  }

  private FormalExamScoringException scoringFailure(
      String message, CompetitionFormalExamQuestionSnapshotEntity question, Throwable cause) {
    Long questionId = question == null ? null : question.getId();
    Long sessionId = question == null ? null : question.getExamSessionId();
    return new FormalExamScoringException(message, sessionId, null, questionId, cause);
  }

  public static String normalizeType(String questionType) {
    if (!StringUtils.hasText(questionType)) {
      return "";
    }
    String type = questionType.trim().toLowerCase(Locale.ROOT);
    if ("multiple".equals(type)) {
      return "multi";
    }
    return type;
  }
}
