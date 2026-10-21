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

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** AI 判分服务：桩 FaqLlmClient / StringRedisTemplate，不依赖真实模型与 Redis（方案 §8.1） */
class AiGradeApplicationServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, String> dictValues = new HashMap<>();
  private FaqLlmClient llmClient;
  private FaqModelConfigApplicationService modelConfigService;
  private DictMapper dictMapper;
  private AiGenerationDraftMapper draftMapper;
  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOps;
  private AiGradeRateLimitService rateLimitService;
  private AiGradeApplicationService service;

  @BeforeAll
  static void initMpTableInfo() {
    // dictInt 的 LambdaQueryWrapper 参数对依赖 DictEntity 的 MP 表信息缓存（无缓存时参数图为空）
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, DictEntity.class);
  }

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
    // 按 code 选择性打桩：grade() 现在读 3 个字典键（判对阈值/闸口开关/汉字阈值），
    // 泛化 any() 打桩会让不同键读到同一个值（闸口新增前 passRateComesFromDict 即受此影响）
    when(dictMapper.selectList(any())).thenAnswer(inv -> {
      if (inv.getArgument(0) instanceof AbstractWrapper<?, ?, ?> wrapper) {
        // MP 3.4.1 参数对惰性合并：先触发 getSqlSegment()，values 才进入 paramNameValuePairs
        wrapper.getSqlSegment();
        String code = wrapper.getParamNameValuePairs().values().stream()
            .filter(String.class::isInstance).map(String.class::cast)
            .filter(s -> s.contains("_")).findFirst().orElse(null);
        if (code != null && dictValues.containsKey(code)) {
          DictEntity dict = new DictEntity();
          dict.setValue(dictValues.get(code));
          return List.of(dict);
        }
      }
      return List.of();
    });
    when(modelConfigService.findByModelCode(eq(10159L), anyString())).thenReturn(modelConfig());
  }

  @AfterEach
  void tearDown() {
    dictValues.clear();
  }

  /** 按字典 code 打桩值（setUp 的 thenAnswer 消费） */
  private void dict(String code, String value) {
    dictValues.put(code, value);
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
    dict("ai_review_grade_pass_rate", "80");
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
  // 无意义作答闸口（拦截方案 §4.2）：不调 LLM、不计限流、不写 draft、不写缓存
  // ------------------------------------------------------------------

  private static final String GUARD_REFERENCE =
      "方向是低碳、零排放、循环经济，实施路径含节能、环保技术及循环经济举措";

  private SubjectiveGradeResult gradeJunk(String junk) {
    return service.grade(10159L, 7L, 501L, "题干：炉衬寿命", GUARD_REFERENCE, junk, "essay", 1);
  }

  @Test
  void guardBlocksAllNineLogJunkAnswersWithoutLlm() {
    // 生产日志 2026-10-21 00:00~00:05 同一学员的 9 个真实垃圾答案全量回归
    for (String junk : new String[]{"2", "3", "4", "5", "6", "Q", "y", "8", "10"}) {
      SubjectiveGradeResult r = gradeJunk(junk);
      assertTrue(r.gradable(), junk);
      assertEquals(0, r.score(), junk);
      assertFalse(r.correct(), junk);
      assertEquals("rule", r.gradeSource(), junk);
      assertTrue(r.comment().contains("记 0 分"), junk);
    }
    verifyNoInteractions(llmClient, rateLimitService, draftMapper);
    // 闸口在缓存之前：缓存零读写
    verify(valueOps, never()).get(anyString());
    verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void guardBlocksJunkHanziPatterns() {
    for (String junk : new String[]{"不会", "不知道", "略", "哈哈", "......"}) {
      SubjectiveGradeResult r = gradeJunk(junk);
      assertEquals("rule", r.gradeSource(), junk);
    }
    verifyNoInteractions(llmClient, rateLimitService, draftMapper);
  }

  @Test
  void guardDisabledByDictFallsBackToLlm() {
    dict("ai_review_grade_guard_enabled", "0");
    stubLlm("{\"score\": 0, \"comment\": \"仅为数字\"}");
    SubjectiveGradeResult r = gradeJunk("2");
    assertEquals("ai", r.gradeSource());
    verify(llmClient).chatCompletionDetailed(anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
  }

  @Test
  void guardExemptionKeepsShortRealAnswerOnLlmPath() {
    // 命中参考答案关键词的言简意赅短答不得被闸口误伤
    stubLlm("{\"score\": 1, \"comment\": \"覆盖要点\"}");
    SubjectiveGradeResult r =
        service.grade(10159L, 7L, 501L, "题干", GUARD_REFERENCE, "低碳、零排放", "essay", 1);
    assertEquals("ai", r.gradeSource());
    assertEquals(1, r.score());
  }

  @Test
  void guardMinHanziThresholdComesFromDict() {
    dict("ai_review_grade_guard_min_hanzi", "6");
    // "我的作答" 4 汉字 < 6 且总长 4 < 8 → 拦截；默认阈值 4 时放行
    SubjectiveGradeResult r = gradeJunk("我的作答");
    assertEquals("rule", r.gradeSource());
    verifyNoInteractions(llmClient);
  }

  @Test
  void ungradableStillTakesPriorityOverGuard() {
    // 无参考答案：ungradable 优先于闸口（维持 Q4-A 口径）
    SubjectiveGradeResult r = service.grade(10159L, 7L, 501L, "题干", "", "2", "essay", 1);
    assertFalse(r.gradable());
    verifyNoInteractions(llmClient, rateLimitService, draftMapper);
  }

  // ------------------------------------------------------------------
  // 入参截断（内部调用方防御）
  // ------------------------------------------------------------------

  @Test
  void overlongTextsTruncatedTo2000() {
    stubLlm("{\"score\": 5, \"comment\": \"好\"}");
    // 非周期内容：R4 重复片段规则会拦截周期性长文本（如单字重复），那是闸口的本职而非误伤
    StringBuilder varied = new StringBuilder();
    for (int i = 0; i < 400; i++) {
      varied.append("要点").append(i).append("，");
    }
    String longText = varied.toString();
    service.grade(10159L, 7L, 501L, longText, longText, longText, "answer", 5);
    ArgumentCaptor<AiGenerationDraftEntity> captor =
        ArgumentCaptor.forClass(AiGenerationDraftEntity.class);
    verify(draftMapper).insert(captor.capture());
    String prompt = captor.getValue().getPrompt();
    assertFalse(prompt.length() > 7000);
  }
}
