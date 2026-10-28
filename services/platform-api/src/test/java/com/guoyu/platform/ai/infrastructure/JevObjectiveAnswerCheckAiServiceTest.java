package com.guoyu.platform.ai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.config.ObjectiveAnswerJevProperties;
import com.guoyu.platform.ai.dto.JevBatchDecisionResult;
import com.guoyu.platform.ai.dto.JevQuestion;
import com.guoyu.platform.ai.dto.JevSubDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCallOutcome;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckCallContext;
import com.guoyu.platform.ai.dto.ObjectiveAnswerDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerOption;
import com.guoyu.platform.ai.dto.ObjectiveAnswerQuestion;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AI 服务（方案 §16.1-D）：请求不泄密、多选分块、整题原子失败、每块审计。
 */
@ExtendWith(MockitoExtension.class)
class JevObjectiveAnswerCheckAiServiceTest {

  @Mock
  private JevDecisionClient client;

  private JevObjectiveAnswerCheckAiService service;

  private static final ObjectiveAnswerCheckCallContext CONTEXT =
      new ObjectiveAnswerCheckCallContext(10159L, 7L, 11L, 22L, 33L, 1);

  @BeforeEach
  void setUp() {
    ObjectiveAnswerJevProperties properties = new ObjectiveAnswerJevProperties();
    properties.setBaseUrl("http://jev.local/gateway");
    properties.setModel("ateve-jev-v1");
    properties.setApiKey("secret-key");
    properties.setReadTimeoutSeconds(30);
    service = new JevObjectiveAnswerCheckAiService(client, properties, new ObjectMapper());
  }

  private ObjectiveAnswerQuestion question(String type, int optionCount) {
    List<ObjectiveAnswerOption> options = new ArrayList<>();
    for (int i = 0; i < optionCount; i++) {
      options.add(new ObjectiveAnswerOption("opt_" + i, i, String.valueOf((char) ('A' + i)),
          "选项文本" + i, i == 0));
    }
    return new ObjectiveAnswerQuestion(101, type, "题干内容", "铝电解工/安全知识", options);
  }

  @SuppressWarnings("unchecked")
  private Map<String, JevQuestion> captureQuestions(int callIndex) {
    ArgumentCaptor<Map<String, JevQuestion>> captor = ArgumentCaptor.forClass(Map.class);
    verify(client, times(callIndex + 1)).decideBatch(anyString(), anyString(), anyString(),
        anyString(), captor.capture(), anyInt());
    return captor.getAllValues().get(callIndex);
  }

  private String captureState(int callIndex) {
    ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
    verify(client, times(callIndex + 1)).decideBatch(anyString(), anyString(), anyString(),
        state.capture(), anyMap(), anyInt());
    return state.getAllValues().get(callIndex);
  }

  private JevBatchDecisionResult singleResult(String choice, Map<String, Double> probabilities) {
    Map<String, JevSubDecision> answers = new LinkedHashMap<>();
    answers.put("answer", new JevSubDecision(choice, 0.9, probabilities, 0.8, 0.9, 0.1));
    return new JevBatchDecisionResult(answers, 10, 0, false, "ateve-jev-v1", "max", null, 5);
  }

  @Test
  void singleRequestNeverLeaksOriginalAnswerOrResolve() {
    Map<String, Double> probabilities = new LinkedHashMap<>();
    probabilities.put("opt_0", 0.2);
    probabilities.put("opt_1", 0.8);
    when(client.decideBatch(anyString(), anyString(), anyString(), anyString(), anyMap(), anyInt()))
        .thenReturn(singleResult("opt_1", probabilities));

    List<ObjectiveAnswerCallOutcome> outcomes = new ArrayList<>();
    ObjectiveAnswerDecision decision = service.check(question("single", 2), CONTEXT,
        outcomes::add);

    assertEquals(ObjectiveAnswerDecision.SCOPE_QUESTION, decision.scope());
    assertEquals("opt_1", decision.choice());
    assertEquals(1, outcomes.size());
    assertEquals("success", outcomes.get(0).status());

    String state = captureState(0);
    Map<String, JevQuestion> questions = captureQuestions(0);
    assertTrue(state.contains("题干内容"));
    assertTrue(state.contains("选项文本0"));
    assertFalse(state.contains("isCorrect"));
    assertFalse(state.contains("正确项"));
    assertFalse(state.contains("resolved"));
    assertTrue(questions.get("answer").criteria().containsKey("opt_0"));
    assertFalse(questions.get("answer").criteria().toString().contains("true"));
  }

  @Test
  void multiChunksEveryFiveOptionsAndKeepsFullContext() {
    when(client.decideBatch(anyString(), anyString(), anyString(), anyString(), anyMap(), anyInt()))
        .thenAnswer(invocation -> {
          Map<String, JevQuestion> questions = invocation.getArgument(4);
          Map<String, JevSubDecision> answers = new LinkedHashMap<>();
          for (String key : questions.keySet()) {
            Map<String, Double> probabilities = new LinkedHashMap<>();
            probabilities.put("Y", 0.8);
            probabilities.put("N", 0.2);
            answers.put(key, new JevSubDecision("Y", 0.9, probabilities, 0.6, 0.8, 0.2));
          }
          return new JevBatchDecisionResult(answers, 20, 0, false, "model", null, null, 5);
        });

    List<ObjectiveAnswerCallOutcome> outcomes = new ArrayList<>();
    ObjectiveAnswerDecision decision = service.check(question("multi", 16), CONTEXT,
        outcomes::add);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, JevQuestion>> questionsCaptor =
        ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    verify(client, times(4)).decideBatch(anyString(), anyString(), anyString(),
        stateCaptor.capture(), questionsCaptor.capture(), anyInt());
    assertEquals(4, decision.chunkCount());
    assertEquals(16, decision.optionDecisions().size());
    assertEquals(4, outcomes.size());
    for (int i = 0; i < 4; i++) {
      Map<String, JevQuestion> questions = questionsCaptor.getAllValues().get(i);
      assertTrue(questions.size() <= 5, "每块最多 5 个多选子问题");
      for (JevQuestion question : questions.values()) {
        assertEquals(Map.of("Y", "应选", "N", "不应选"), question.criteria());
      }
      String state = stateCaptor.getAllValues().get(i);
      for (int option = 0; option < 16; option++) {
        assertTrue(state.contains("选项文本" + option), "每块 state 必须包含全部选项");
      }
    }
  }

  @Test
  void multiWithSixOptionsUsesTwoChunks() {
    when(client.decideBatch(anyString(), anyString(), anyString(), anyString(), anyMap(), anyInt()))
        .thenAnswer(invocation -> {
          Map<String, JevQuestion> questions = invocation.getArgument(4);
          Map<String, JevSubDecision> answers = new LinkedHashMap<>();
          for (String key : questions.keySet()) {
            answers.put(key, new JevSubDecision("N", 0.9,
                Map.of("Y", 0.1, "N", 0.9), 0.8, 0.9, 0.1));
          }
          return new JevBatchDecisionResult(answers, 20, 0, false, null, null, null, 5);
        });
    service.check(question("multi", 6), CONTEXT, outcome -> {
    });
    verify(client, times(2)).decideBatch(anyString(), anyString(), anyString(), anyString(),
        anyMap(), anyInt());
  }

  @Test
  void secondChunkFailureProducesNoPartialDecisionAndAuditsBothChunks() {
    when(client.decideBatch(anyString(), anyString(), anyString(), anyString(), anyMap(), anyInt()))
        .thenAnswer(invocation -> {
          Map<String, JevQuestion> questions = invocation.getArgument(4);
          if (questions.containsKey("opt_5")) {
            throw new JevServerException("boom");
          }
          Map<String, JevSubDecision> answers = new LinkedHashMap<>();
          for (String key : questions.keySet()) {
            answers.put(key, new JevSubDecision("Y", 0.9,
                Map.of("Y", 0.9, "N", 0.1), 0.8, 0.9, 0.1));
          }
          return new JevBatchDecisionResult(answers, 20, 0, false, null, null, null, 5);
        });

    List<ObjectiveAnswerCallOutcome> outcomes = new ArrayList<>();
    ObjectiveAnswerCheckAiException error = assertThrows(ObjectiveAnswerCheckAiException.class,
        () -> service.check(question("multi", 9), CONTEXT, outcomes::add));

    assertTrue(error.isRetryable());
    assertEquals(2, outcomes.size(), "前序成功块与当前失败块都要审计");
    assertEquals("success", outcomes.get(0).status());
    assertEquals("failed", outcomes.get(1).status());
    assertEquals(2, outcomes.get(1).chunkIndex());
    assertEquals(2, outcomes.get(1).chunkCount());
  }

  @Test
  void judgeUsesTruthValueCriteriaAndMapsBackToOptionId() {
    Map<String, Double> probabilities = new LinkedHashMap<>();
    probabilities.put("T", 0.85);
    probabilities.put("F", 0.15);
    when(client.decideBatch(anyString(), anyString(), anyString(), anyString(), anyMap(), anyInt()))
        .thenReturn(singleResult("T", probabilities));

    ObjectiveAnswerQuestion judge = new ObjectiveAnswerQuestion(102, "judge", "判断题干", null,
        List.of(new ObjectiveAnswerOption("opt_0", 0, "T", "正确", true),
            new ObjectiveAnswerOption("opt_1", 1, "F", "错误", false)));
    ObjectiveAnswerDecision decision = service.check(judge, CONTEXT, outcome -> {
    });

    assertEquals("opt_0", decision.choice());
    assertEquals(0.85, decision.probabilities().get("opt_0"), 1e-9);
    Map<String, JevQuestion> questions = captureQuestions(0);
    assertTrue(questions.get("answer").criteria().containsKey("T"));
    assertTrue(questions.get("answer").criteria().containsKey("F"));
  }

  @Test
  void missingCredentialsRejectsWithoutHttpCall() {
    ObjectiveAnswerJevProperties properties = new ObjectiveAnswerJevProperties();
    properties.setApiKey("");
    JevObjectiveAnswerCheckAiService noKey = new JevObjectiveAnswerCheckAiService(client,
        properties, new ObjectMapper());
    ObjectiveAnswerCheckAiException error = assertThrows(ObjectiveAnswerCheckAiException.class,
        () -> noKey.check(question("single", 2), CONTEXT, outcome -> {
        }));
    assertEquals("NOT_CONFIGURED", error.getErrorCode());
    verify(client, times(0)).decideBatch(anyString(), anyString(), anyString(), anyString(),
        anyMap(), eq(30));
  }
}
