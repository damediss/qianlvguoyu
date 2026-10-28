package com.guoyu.platform.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.config.ObjectiveAnswerJevProperties;
import com.guoyu.platform.ai.dto.JevBatchDecisionResult;
import com.guoyu.platform.ai.dto.JevQuestion;
import com.guoyu.platform.ai.dto.JevSubDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCallOutcome;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckAiDescriptor;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckCallContext;
import com.guoyu.platform.ai.dto.ObjectiveAnswerDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOption;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOptionDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerQuestion;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiException;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JEV 版客观题作答（方案 §6 / §9）。
 *
 * <p>单选/判断一次逻辑调用；多选每个选项一个二元子问题、每 5 个选项一块。
 * 每个逻辑块结束立即回调 auditSink（成功与失败都要），审计写失败会使整题失败。
 * 任何一块失败都不产生半份 AI 答案。
 */
@Slf4j
@Service
public class JevObjectiveAnswerCheckAiService implements ObjectiveAnswerCheckAiService {

  /** 每块最多 5 个多选子问题——固定常量，不开放环境配置（方案 §9.3）。 */
  static final int MULTI_CHUNK_SIZE = 5;

  private static final String PROMPT_VERSION = "objective-answer-v1";

  private final JevDecisionClient client;
  private final ObjectiveAnswerJevProperties properties;
  private final ObjectMapper objectMapper;

  public JevObjectiveAnswerCheckAiService(JevDecisionClient client,
      ObjectiveAnswerJevProperties properties, ObjectMapper objectMapper) {
    this.client = client;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public ObjectiveAnswerCheckAiDescriptor descriptor() {
    return new ObjectiveAnswerCheckAiDescriptor(properties.getModel(),
        properties.apiKeyOrNull() != null && properties.baseUrlOrNull() != null);
  }

  @Override
  public ObjectiveAnswerDecision check(ObjectiveAnswerQuestion question,
      ObjectiveAnswerCheckCallContext context, Consumer<ObjectiveAnswerCallOutcome> auditSink) {
    if (!descriptor().credentialsReady()) {
      throw unavailable();
    }
    if ("multi".equals(question.questionType())) {
      return checkMulti(question, context, auditSink);
    }
    return checkSingleOrJudge(question, context, auditSink);
  }

  private ObjectiveAnswerDecision checkSingleOrJudge(ObjectiveAnswerQuestion question,
      ObjectiveAnswerCheckCallContext context, Consumer<ObjectiveAnswerCallOutcome> auditSink) {
    List<ObjectiveAnswerOption> ordered = rotateForQuestion(question);
    boolean judge = "judge".equals(question.questionType());
    String state = buildState(question, ordered, judge);
    Map<String, String> criteria = new LinkedHashMap<>();
    if (judge) {
      for (ObjectiveAnswerOption option : ordered) {
        criteria.put(option.label(), option.text());
      }
    } else {
      for (ObjectiveAnswerOption option : ordered) {
        criteria.put(option.optionId(), option.text());
      }
    }
    String instructions = judge ? "判断题干的陈述是否正确。" : "根据题干和选项独立作答，选择唯一最符合题意的选项。";
    Map<String, JevQuestion> questions = new LinkedHashMap<>();
    questions.put("answer", new JevQuestion(instructions, criteria));
    String promptSha = promptSha256(state, questions);
    String callId = UUID.randomUUID().toString();

    JevBatchDecisionResult result;
    try {
      result = client.decideBatch(properties.baseUrlOrNull(), properties.apiKeyOrNull(),
          properties.getModel(), state, questions, properties.getReadTimeoutSeconds());
    } catch (RuntimeException e) {
      auditSink.accept(failedOutcome(callId, context, 1, 1, question.questionType(), promptSha,
          question, e));
      throw toAiException(e);
    }
    auditSink.accept(successOutcome(callId, context, 1, 1, question.questionType(), promptSha,
        question, result));

    JevSubDecision sub = result.answers().get("answer");
    if (sub == null) {
      throw new ObjectiveAnswerCheckAiException("模型未返回答案", "RESPONSE_INVALID", true, true,
          null);
    }
    Map<String, Double> probabilities = new LinkedHashMap<>();
    if (judge) {
      for (Map.Entry<String, Double> entry : sub.probabilities().entrySet()) {
        String optionId = optionIdByJudgeLabel(ordered, entry.getKey());
        if (optionId != null) {
          probabilities.put(optionId, entry.getValue());
        }
      }
    } else {
      probabilities.putAll(sub.probabilities());
    }
    String choice = judge ? optionIdByJudgeLabel(ordered, sub.choice()) : sub.choice();
    if (choice == null) {
      throw new ObjectiveAnswerCheckAiException("模型返回的选项无法映射", "RESPONSE_INVALID", true,
          true, null);
    }
    return new ObjectiveAnswerDecision(ObjectiveAnswerDecision.SCOPE_QUESTION, choice,
        sub.confidence(), probabilities, Map.of(), 1);
  }

  private ObjectiveAnswerDecision checkMulti(ObjectiveAnswerQuestion question,
      ObjectiveAnswerCheckCallContext context, Consumer<ObjectiveAnswerCallOutcome> auditSink) {
    List<ObjectiveAnswerOption> options = question.options();
    int chunkCount = (options.size() + MULTI_CHUNK_SIZE - 1) / MULTI_CHUNK_SIZE;
    String state = buildState(question, options, false);
    Map<String, ObjectiveAnswerOptionDecision> decisions = new LinkedHashMap<>();
    int chunkIndex = 0;
    for (int start = 0; start < options.size(); start += MULTI_CHUNK_SIZE) {
      chunkIndex++;
      List<ObjectiveAnswerOption> chunk = options.subList(start,
          Math.min(options.size(), start + MULTI_CHUNK_SIZE));
      Map<String, JevQuestion> questions = new LinkedHashMap<>();
      for (ObjectiveAnswerOption option : chunk) {
        questions.put(option.optionId(), new JevQuestion(
            "结合题干和全部选项，判断选项 " + option.label() + " 是否属于本题正确答案。",
            Map.of("Y", "应选", "N", "不应选")));
      }
      String promptSha = promptSha256(state, questions);
      String callId = UUID.randomUUID().toString();
      JevBatchDecisionResult result;
      try {
        result = client.decideBatch(properties.baseUrlOrNull(), properties.apiKeyOrNull(),
            properties.getModel(), state, questions, properties.getReadTimeoutSeconds());
      } catch (RuntimeException e) {
        auditSink.accept(failedOutcome(callId, context, chunkIndex, chunkCount,
            question.questionType(), promptSha, question, e));
        throw toAiException(e);
      }
      auditSink.accept(successOutcome(callId, context, chunkIndex, chunkCount,
          question.questionType(), promptSha, question, result));

      for (ObjectiveAnswerOption option : chunk) {
        JevSubDecision sub = result.answers().get(option.optionId());
        if (sub == null) {
          throw new ObjectiveAnswerCheckAiException(
              "模型未返回选项 " + option.label() + " 的判断", "RESPONSE_INVALID", true, true, null);
        }
        decisions.put(option.optionId(), new ObjectiveAnswerOptionDecision(option.optionId(),
            sub.choice(), sub.confidence(), sub.probabilities(), sub.margin(), sub.top1(),
            sub.top2()));
      }
    }
    return new ObjectiveAnswerDecision(ObjectiveAnswerDecision.SCOPE_OPTION, null, null, null,
        decisions, chunkCount);
  }

  private List<ObjectiveAnswerOption> rotateForQuestion(ObjectiveAnswerQuestion question) {
    List<ObjectiveAnswerOption> options = new ArrayList<>(question.options());
    int size = options.size();
    if (size == 0) {
      return options;
    }
    if ("judge".equals(question.questionType())) {
      if (question.detailId() != null && question.detailId() % 2 == 1) {
        options.sort((a, b) -> Boolean.compare(!"F".equals(a.label()), !"F".equals(b.label())));
      }
      return options;
    }
    int rotate = Math.floorMod(question.detailId() == null ? 0 : question.detailId(), size);
    List<ObjectiveAnswerOption> rotated = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      rotated.add(options.get((i + rotate) % size));
    }
    return rotated;
  }

  private String buildState(ObjectiveAnswerQuestion question, List<ObjectiveAnswerOption> options,
      boolean judge) {
    StringBuilder sb = new StringBuilder();
    if (question.contextPath() != null && !question.contextPath().isBlank()) {
      sb.append("【背景】").append(question.contextPath()).append('\n');
    }
    if (judge) {
      sb.append("【题型】判断题\n");
    } else if ("multi".equals(question.questionType())) {
      sb.append("【题型】多选题，至少有两个正确选项\n");
    } else {
      sb.append("【题型】单选题\n");
    }
    sb.append("【题干】").append(question.stemText()).append('\n');
    sb.append(judge ? "【选项】\n" : "【全部选项】\n");
    for (ObjectiveAnswerOption option : options) {
      if (!judge) {
        sb.append(option.label()).append(". ");
      }
      sb.append(option.text()).append('\n');
    }
    return sb.toString();
  }

  private String optionIdByJudgeLabel(List<ObjectiveAnswerOption> options, String label) {
    if (label == null) {
      return null;
    }
    String normalized = label.trim().toUpperCase(Locale.ROOT);
    for (ObjectiveAnswerOption option : options) {
      if (option.label().equalsIgnoreCase(normalized)) {
        return option.optionId();
      }
    }
    return null;
  }

  private ObjectiveAnswerCallOutcome successOutcome(String callId,
      ObjectiveAnswerCheckCallContext context, int chunkIndex, int chunkCount, String questionType,
      String promptSha, ObjectiveAnswerQuestion question, JevBatchDecisionResult result) {
    return new ObjectiveAnswerCallOutcome(callId, context.batchId(), context.itemId(),
        question.detailId(), context.runId(), context.claimAttempt(), chunkIndex, chunkCount,
        questionType, properties.getModel(), result.responseModel(),
        result.confidenceDefinition(), promptSha, preview(question.stemText()),
        result.inputTokens(), result.outputTokens(), result.usageEstimated(), result.durationMs(),
        ObjectiveAnswerCallOutcome.STATUS_SUCCESS, null, null);
  }

  private ObjectiveAnswerCallOutcome failedOutcome(String callId,
      ObjectiveAnswerCheckCallContext context, int chunkIndex, int chunkCount, String questionType,
      String promptSha, ObjectiveAnswerQuestion question, RuntimeException error) {
    return new ObjectiveAnswerCallOutcome(callId, context.batchId(), context.itemId(),
        question.detailId(), context.runId(), context.claimAttempt(), chunkIndex, chunkCount,
        questionType, properties.getModel(), null, null, promptSha, preview(question.stemText()),
        null, null, false, null, ObjectiveAnswerCallOutcome.STATUS_FAILED, classify(error),
        truncate(error.getMessage(), 500));
  }

  private String preview(String stem) {
    if (stem == null) {
      return "";
    }
    String flat = stem.replaceAll("\\s+", " ");
    return flat.length() <= 200 ? flat : flat.substring(flat.length() - 200);
  }

  private String promptSha256(String state, Map<String, JevQuestion> questions) {
    try {
      Map<String, Object> canonical = new LinkedHashMap<>();
      canonical.put("promptVersion", PROMPT_VERSION);
      canonical.put("state", state);
      canonical.put("questions", questions);
      String json = objectMapper.writeValueAsString(canonical);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("计算 promptSha256 失败", e);
    }
  }

  private String classify(RuntimeException error) {
    if (error instanceof JevBudgetExceededException) {
      return "TOKEN_BUDGET_EXCEEDED";
    }
    if (error instanceof JevUnauthorizedException) {
      return "UNAUTHORIZED";
    }
    if (error instanceof JevValidationException) {
      return "REQUEST_INVALID";
    }
    if (error instanceof JevParseException) {
      return "RESPONSE_INVALID";
    }
    return "UPSTREAM_UNAVAILABLE";
  }

  private ObjectiveAnswerCheckAiException toAiException(RuntimeException error) {
    if (error instanceof ObjectiveAnswerCheckAiException ai) {
      return ai;
    }
    if (error instanceof JevBudgetExceededException) {
      return new ObjectiveAnswerCheckAiException("内容超出模型输入上限", "TOKEN_BUDGET_EXCEEDED",
          false, false, error);
    }
    if (error instanceof JevUnauthorizedException) {
      return new ObjectiveAnswerCheckAiException("模型服务鉴权失败，请联系管理员检查配置", "UNAUTHORIZED",
          true, true, error);
    }
    if (error instanceof JevValidationException) {
      return new ObjectiveAnswerCheckAiException("请求内容不符合模型要求", "REQUEST_INVALID", false,
          true, error);
    }
    if (error instanceof JevParseException) {
      return new ObjectiveAnswerCheckAiException("模型返回内容异常，可稍后重试", "RESPONSE_INVALID", true,
          true, error);
    }
    return new ObjectiveAnswerCheckAiException("模型服务暂时不可用，可稍后重试",
        "UPSTREAM_UNAVAILABLE", true, true, error);
  }

  private ObjectiveAnswerCheckAiException unavailable() {
    return new ObjectiveAnswerCheckAiException("答案 AI 检查暂未开通，请联系管理员。",
        "NOT_CONFIGURED", false, false, null);
  }

  private String truncate(String text, int max) {
    if (text == null) {
      return null;
    }
    return text.length() <= max ? text : text.substring(0, max);
  }
}
