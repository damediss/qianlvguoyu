package com.guoyu.platform.exam.reviewsync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.infrastructure.FaqLlmResponseParser;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.reviewsync.service.AiGradeApplicationService.SubjectiveGradeResult;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** AI 判分服务：桩 FaqLlmClient / StringRedisTemplate，不依赖真实模型与 Redis（方案 §8.1） */
class AiGradeApplicationServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private FaqLlmClient llmClient;
  private FaqModelConfigApplicationService modelConfigService;
  private DictMapper dictMapper;
  private AiGenerationDraftMapper draftMapper;
  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOps;
  private AiGradeRateLimitService rateLimitService;
  private AiGradeApplicationService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    llmClient = mock(FaqLlmClient.class);
    modelConfigService = mock(FaqModelConfigApplicationService.class);
    dictMapper = mock(DictMapper.class);
    draftMapper = mock(AiGenerationDraftMapper.class);
    redisTemplate = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    rateLimitService = mock(AiGradeRateLimitService.class);
    service = new AiGradeApplicationService(
        llmClient, new FaqLlmResponseParser(objectMapper), modelConfigService, dictMapper,
        draftMapper, redisTemplate, rateLimitService, objectMapper);

    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(dictMapper.selectList(any())).thenReturn(List.of());
    when(modelConfigService.findByModelCode(eq(10159L), anyString())).thenReturn(modelConfig());
  }

  private ExamFaqModelConfigEntity modelConfig() {
    ExamFaqModelConfigEntity config = new ExamFaqModelConfigEntity();
    config.setBaseUrl("https://api.deepseek.com");
    config.setApiKey("k");
    config.setModelId("deepseek-flash");
    return config;
  }

  private void stubLlm(String... contents) {
    LlmChatCompletionResult[] results = new LlmChatCompletionResult[contents.length];
    for (int i = 0; i < contents.length; i++) {
      results[i] = LlmChatCompletionResult.empty(contents[i]);
    }
    if (results.length == 1) {
      when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
          anyString(), anyInt(), any(), any(), anyBoolean(), any(), anyInt()))
          .thenReturn(results[0]);
    } else {
      when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
          anyString(), anyInt(), any(), any(), anyBoolean(), any(), anyInt()))
          .thenReturn(results[0], java.util.Arrays.copyOfRange(results, 1, results.length));
    }
  }

  private SubjectiveGradeResult grade(int fullScore) {
    return service.grade(10159L, 7L, 501L, "题干：铝电解原理", "参考答案：电解氧化铝", "我的作答",
        "answer", fullScore);
  }

  // ------------------------------------------------------------------
  // 白名单
  // ------------------------------------------------------------------

  @Test
  void subjectiveWhitelist() {
    for (String type : new String[]{"answer", "cal", "essay", "analysis", "explain", "fill",
        "short", "drafting"}) {
      assertTrue(AiGradeApplicationService.isSubjective(type), type);
    }
    assertTrue(AiGradeApplicationService.isSubjective(" Answer "));
    assertFalse(AiGradeApplicationService.isSubjective("single"));
    assertFalse(AiGradeApplicationService.isSubjective("multi"));
    assertFalse(AiGradeApplicationService.isSubjective("judge"));
    assertFalse(AiGradeApplicationService.isSubjective(""));
    assertFalse(AiGradeApplicationService.isSubjective(null));
  }

  // ------------------------------------------------------------------
  // 不可判分（无参考答案）
  // ------------------------------------------------------------------

  @Test
  void ungradableWhenReferenceEmpty() {
    SubjectiveGradeResult r = service.grade(10159L, 7L, 501L, "题干", "", "作答", "answer", 5);
    assertFalse(r.gradable());
    verifyNoInteractions(llmClient, rateLimitService, draftMapper);
  }

  @Test
  void ungradableWhenReferenceExtractionFails() {
    SubjectiveGradeResult r =
        service.grade(10159L, 7L, 501L, "题干", "[{\"content\":\"\"}]", "作答", "answer", 5);
    assertFalse(r.gradable());
    verifyNoInteractions(llmClient, rateLimitService, draftMapper);
  }

  // ------------------------------------------------------------------
  // 钳位与判对阈值
  // ------------------------------------------------------------------

  @Test
  void scoreClampedToFullScore() {
    stubLlm("{\"score\": 99, \"comment\": \"很好\"}");
    SubjectiveGradeResult r = grade(5);
    assertTrue(r.gradable());
    assertEquals(5, r.score());
    assertTrue(r.correct());
    assertEquals("ai", r.gradeSource());
  }

  @Test
  void scoreClampedToZero() {
    stubLlm("{\"score\": -3, \"comment\": \"不对\"}");
    SubjectiveGradeResult r = grade(5);
    assertEquals(0, r.score());
    assertFalse(r.correct());
  }

  @Test
  void passAtSixtyPercent() {
    stubLlm("{\"score\": 3, \"comment\": \"基本正确\"}");
    SubjectiveGradeResult r = grade(5);
    assertEquals(3, r.score());
    assertTrue(r.correct());
  }

  @Test
  void failBelowSixtyPercent() {
    stubLlm("{\"score\": 2, \"comment\": \"要点不全\"}");
    SubjectiveGradeResult r = grade(5);
    assertFalse(r.correct());
  }

  @Test
  void passRateComesFromDict() {
    DictEntity dict = new DictEntity();
    dict.setValue("80");
    when(dictMapper.selectList(any())).thenReturn(List.of(dict));
    stubLlm("{\"score\": 3, \"comment\": \"基本正确\"}");
    // 3/5 = 60% < 80% → 判错（默认 60 时判对）
    SubjectiveGradeResult r = grade(5);
    assertFalse(r.correct());
  }

  @Test
  void fullScoreZeroGuardedAgainstDivisionByZero() {
    stubLlm("{\"score\": 5, \"comment\": \"好\"}");
    SubjectiveGradeResult r = grade(0);
    assertEquals(0, r.score());
    assertFalse(r.correct());
  }

  // ------------------------------------------------------------------
  // LLM 调用参数（Q7：thinking=disabled、maxTokens=2048、jsonMode、超时 30s）
  // ------------------------------------------------------------------

  @Test
  void llmCalledWithGradeParameters() {
    stubLlm("{\"score\": 4, \"comment\": \"不错\"}");
    grade(5);
    verify(llmClient).chatCompletionDetailed(
        eq("https://api.deepseek.com"), eq("k"), eq("deepseek-flash"),
        anyString(), anyString(),
        eq(2048), eq("disabled"), isNull(), eq(true), eq(10159L), eq(30));
  }

  // ------------------------------------------------------------------
  // 输出零信任：解析失败重试 1 次后按失败处理
  // ------------------------------------------------------------------

  @Test
  void parseFailureRetriesOnceThenThrows() {
    stubLlm("这不是 JSON");
    BusinessException ex = assertThrows(BusinessException.class, () -> grade(5));
    assertEquals(ErrorCode.SERVICE_UNAVAILABLE, ex.getCode());
    verify(llmClient, times(2)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
    verify(draftMapper, never()).insert(any(AiGenerationDraftEntity.class));
  }

  @Test
  void nonNumericScoreRetriesOnceThenThrows() {
    stubLlm("{\"score\": \"满分\", \"comment\": \"x\"}");
    assertThrows(BusinessException.class, () -> grade(5));
    verify(llmClient, times(2)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
  }

  @Test
  void parseFailureRecoversOnRetry() {
    stubLlm("garbage", "{\"score\": 5, \"comment\": \"第二次成功\"}");
    SubjectiveGradeResult r = grade(5);
    assertTrue(r.gradable());
    assertEquals(5, r.score());
    assertTrue(r.correct());
    verify(llmClient, times(2)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
  }

  @Test
  void markdownWrappedJsonParsed() {
    stubLlm("```json\n{\"score\": 4, \"comment\": \"可以\"}\n```");
    SubjectiveGradeResult r = grade(5);
    assertEquals(4, r.score());
  }

  // ------------------------------------------------------------------
  // 判分缓存
  // ------------------------------------------------------------------

  @Test
  void cacheHitSkipsLlmAndRateLimit() throws Exception {
    String cachedJson = objectMapper.writeValueAsString(
        new SubjectiveGradeResult(true, 4, true, "缓存点评", "ai"));
    when(valueOps.get(anyString())).thenReturn(cachedJson);

    SubjectiveGradeResult r = grade(5);
    assertTrue(r.gradable());
    assertEquals(4, r.score());
    assertTrue(r.correct());
    assertEquals("缓存点评", r.comment());
    verifyNoInteractions(llmClient, rateLimitService, draftMapper);
  }

  @Test
  void cacheMissWritesCacheAndCountsRateLimit() {
    stubLlm("{\"score\": 5, \"comment\": \"好\"}");
    grade(5);
    verify(rateLimitService).acquire(10159L, 7L);
    verify(valueOps).set(startsWith("grade:10159:501:"), anyString(), eq(Duration.ofDays(7)));
  }

  // ------------------------------------------------------------------
  // 限流
  // ------------------------------------------------------------------

  @Test
  void rateLimitExceededThrowsAndSkipsLlm() {
    doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "AI 判分次数已达上限，请自评"))
        .when(rateLimitService).acquire(any(), any());
    BusinessException ex = assertThrows(BusinessException.class, () -> grade(5));
    assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
    verifyNoInteractions(llmClient);
    verify(draftMapper, never()).insert(any(AiGenerationDraftEntity.class));
  }

  // ------------------------------------------------------------------
  // 模型配置缺失
  // ------------------------------------------------------------------

  @Test
  void missingModelConfigThrows() {
    when(modelConfigService.findByModelCode(eq(10159L), anyString())).thenReturn(null);
    BusinessException ex = assertThrows(BusinessException.class, () -> grade(5));
    assertEquals(ErrorCode.SERVICE_UNAVAILABLE, ex.getCode());
    verifyNoInteractions(llmClient);
  }

  // ------------------------------------------------------------------
  // 审计
  // ------------------------------------------------------------------

  @Test
  void auditDraftWrittenWithOperator() {
    stubLlm("{\"score\": 4, \"comment\": \"不错\"}");
    grade(5);
    ArgumentCaptor<AiGenerationDraftEntity> captor =
        ArgumentCaptor.forClass(AiGenerationDraftEntity.class);
    verify(draftMapper).insert(captor.capture());
    AiGenerationDraftEntity draft = captor.getValue();
    assertEquals(10159L, draft.getTenantId());
    assertEquals("ai_review_grade", draft.getDraftType());
    assertEquals("exam_detail", draft.getSourceType());
    assertEquals(501L, draft.getSourceId());
    assertEquals("deepseek-flash", draft.getModelName());
    assertEquals("generated", draft.getStatus());
    assertEquals(7L, draft.getCreatedBy());
    assertEquals(7L, draft.getUpdatedBy());
    assertEquals(0, draft.getDeleted());
    assertTrue(draft.getPrompt().contains("铝电解原理"));
    assertTrue(draft.getResultText().contains("\"score\": 4"));
  }

  // ------------------------------------------------------------------
  // 入参截断（内部调用方防御）
  // ------------------------------------------------------------------

  @Test
  void overlongTextsTruncatedTo2000() {
    stubLlm("{\"score\": 5, \"comment\": \"好\"}");
    String longStem = "题".repeat(3000);
    String longRef = "参".repeat(3000);
    String longAnswer = "答".repeat(3000);
    service.grade(10159L, 7L, 501L, longStem, longRef, longAnswer, "answer", 5);
    ArgumentCaptor<AiGenerationDraftEntity> captor =
        ArgumentCaptor.forClass(AiGenerationDraftEntity.class);
    verify(draftMapper).insert(captor.capture());
    String prompt = captor.getValue().getPrompt();
    assertFalse(prompt.contains("题".repeat(2001)));
    assertFalse(prompt.contains("参".repeat(2001)));
    assertFalse(prompt.contains("答".repeat(2001)));
  }
}
