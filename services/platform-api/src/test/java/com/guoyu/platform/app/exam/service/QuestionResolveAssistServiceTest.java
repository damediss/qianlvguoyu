package com.guoyu.platform.app.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.exam.service.OfficialResolveResolver;
import com.guoyu.platform.exam.reviewsync.service.ReviewQuestionQueryService;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory;
import com.guoyu.platform.exam.service.QuestionResolveRateLimitService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 题目解析统一入口（《试题解析Markdown表格化_一期实施方案》§5.3~§5.4、§9 服务用例）。
 * 内存 draft 存储 + code-aware 字典桩 + Redis 桩，不依赖真实模型与 Redis。
 */
class QuestionResolveAssistServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, String> dictValues = new HashMap<>();
  private final List<AiGenerationDraftEntity> drafts = new ArrayList<>();
  private final Map<String, String> failStore = new java.util.HashMap<>();
  private final List<Object> resolveUpdateWrappers = new ArrayList<>();
  private final AtomicLong draftIdSeq = new AtomicLong(100);

  private ExamExamDetailMapper examDetailMapper;
  private FaqLlmClient llmClient;
  private AiGenerationDraftMapper draftMapper;
  private QuestionResolveRateLimitService rateLimitService;
  private ReviewQuestionQueryService reviewQuestionQueryService;
  private StringRedisTemplate redisTemplate;
  private ExamKnowledgeStructureMapper knowledgeMapper;
  private FaqModelConfigApplicationService modelConfigService;
  @SuppressWarnings("unchecked")
  private ValueOperations<String, String> valueOps;
  private QuestionResolveAssistService service;

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, DictEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiGenerationDraftEntity.class);
    // backfillResolveColumn 的 LambdaUpdateWrapper 方法引用依赖 ExamExamDetailEntity 表信息缓存
    TableInfoHelper.initTableInfo(assistant, ExamExamDetailEntity.class);
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    dictValues.clear();
    drafts.clear();
    failStore.clear();
    resolveUpdateWrappers.clear();
    examDetailMapper = mock(ExamExamDetailMapper.class);
    llmClient = mock(FaqLlmClient.class);
    modelConfigService = mock(FaqModelConfigApplicationService.class);
    DictMapper dictMapper = mock(DictMapper.class);
    draftMapper = mock(AiGenerationDraftMapper.class);
    knowledgeMapper = mock(ExamKnowledgeStructureMapper.class);
    rateLimitService = mock(QuestionResolveRateLimitService.class);
    reviewQuestionQueryService = mock(ReviewQuestionQueryService.class);
    redisTemplate = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);

    FollowTaskBankQuestionConverter converter =
        new FollowTaskBankQuestionConverter(objectMapper);
    QuestionResolveInputFactory inputFactory =
        new QuestionResolveInputFactory(converter, knowledgeMapper, objectMapper);
    OfficialResolveResolver resolver = new OfficialResolveResolver(draftMapper, inputFactory, objectMapper);
    service = new QuestionResolveAssistService(
        examDetailMapper, converter, llmClient, modelConfigService, dictMapper, draftMapper,
        resolver, inputFactory, rateLimitService, reviewQuestionQueryService, redisTemplate);

    when(dictMapper.selectList(any())).thenAnswer(inv -> {
      if (inv.getArgument(0) instanceof AbstractWrapper<?, ?, ?> wrapper) {
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
    dict("ai_review_model_code", "deepseek-review");
    ExamFaqModelConfigEntity config = new ExamFaqModelConfigEntity();
    config.setBaseUrl("https://api.deepseek.com");
    config.setApiKey("k");
    config.setModelId("deepseek-flash");
    config.setThinking("disabled");
    when(modelConfigService.findByModelCode(10159L, "deepseek-review")).thenReturn(config);

    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(valueOps.get(anyString())).thenAnswer(inv -> failStore.get(inv.getArgument(0)));
    doAnswer(inv -> {
      failStore.put(inv.getArgument(0), inv.getArgument(1));
      return null;
    }).when(valueOps).set(anyString(), anyString(), any(Duration.class));
    doReturn(1L).when(redisTemplate)
        .execute(any(DefaultRedisScript.class), anyList(), any());

    doAnswer(inv -> {
      AiGenerationDraftEntity draft = inv.getArgument(0);
      draft.setId(draftIdSeq.incrementAndGet());
      drafts.add(draft);
      return 1;
    }).when(draftMapper).insert(any(AiGenerationDraftEntity.class));
    when(draftMapper.selectOne(any())).thenAnswer(inv -> {
      if (!(inv.getArgument(0) instanceof AbstractWrapper<?, ?, ?> wrapper)) {
        return null;
      }
      wrapper.getSqlSegment();
      Map<String, Object> pairs = wrapper.getParamNameValuePairs();
      Long sourceId = pairs.values().stream()
          .filter(Number.class::isInstance).map(Number.class::cast)
          .map(Number::longValue).findFirst().orElse(null);
      String status = pairs.values().stream()
          .filter(String.class::isInstance).map(String.class::cast)
          .filter(s -> s.equals("generated") || s.equals("shadow") || s.equals("rejected"))
          .findFirst().orElse(null);
      if (sourceId == null || status == null) {
        return null;
      }
      return drafts.stream()
          .filter(d -> sourceId.equals(d.getSourceId()) && status.equals(d.getStatus())
              && d.getDeleted() != null && d.getDeleted() == 0)
          .max(java.util.Comparator.comparing(AiGenerationDraftEntity::getId))
          .orElse(null);
    });
    // 条件更新回写 resolve：捕获 update(null, wrapper)（any() 含 null 实体）
    doAnswer(inv -> {
      resolveUpdateWrappers.add(inv.getArgument(1));
      return 1;
    }).when(examDetailMapper).update(any(), any());
    doNothing().when(rateLimitService).acquire(any(), any());

    TenantContextHolder.setTenantId(10159L);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void dbColumnHitReturnsWithoutLlm() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setResolve("人工解析文本");
    when(examDetailMapper.selectById(11)).thenReturn(detail);

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("db", out.get("source"));
    assertEquals("人工解析文本", out.get("resolve"));
    assertEquals(Boolean.FALSE, out.get("generated"));
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
    assertTrue(drafts.isEmpty());
  }

  @Test
  void answerJsonHitReturnsOfficialWithoutLlm() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setAnswer("{\"analysis\":\"官方解析文本\"}");
    when(examDetailMapper.selectById(11)).thenReturn(detail);

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("official", out.get("source"));
    assertEquals("官方解析文本", out.get("resolve"));
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void firstCallWritesShadowDraftSecondCallZeroLlm() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(resolveText());

    Map<String, Object> first = service.ensureResolve(11);

    assertEquals("shadow", first.get("source"));
    assertEquals(Boolean.TRUE, first.get("generated"));
    assertEquals(resolveText().trim(), first.get("resolve"));
    // shadow 模式：只写 draft，不回写 resolve 列
    verify(examDetailMapper, never()).update(any(), any());
    assertEquals(1, drafts.size());
    assertEquals("shadow", drafts.get(0).getStatus());
    assertTrue(drafts.get(0).getPrompt().startsWith("question_resolve_v2.1#"));

    Map<String, Object> second = service.ensureResolve(11);

    assertEquals("shadow", second.get("source"));
    assertEquals(Boolean.FALSE, second.get("generated"));
    verify(llmClient, times(1)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void writebackModePersistsFullTextWithoutTruncationAndSecondCallReadsCache() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");
    String longText = longResolveText();
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(longText);

    Map<String, Object> first = service.ensureResolve(11);

    assertEquals("ai", first.get("source"));
    assertEquals(longText.trim(), first.get("resolve"));
    // 无 250 截断：全文经条件更新落列
    assertEquals(1, resolveUpdateWrappers.size());
    assertEquals(longText.trim(), resolveSetValue(resolveUpdateWrappers.get(0)));
    assertTrue(longText.trim().length() > 250);
    // 回写成功才失效同步学 6h 缓存
    verify(reviewQuestionQueryService).evictQuestionDetail(11L);
    assertEquals(1, drafts.size());
    assertEquals("generated", drafts.get(0).getStatus());

    Map<String, Object> second = service.ensureResolve(11);

    assertEquals("draft", second.get("source"));
    assertEquals(longText.trim(), second.get("resolve"));
    verify(llmClient, times(1)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void generationDisabledReturnsEmptyDisabled() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("empty", out.get("source"));
    assertEquals("disabled", out.get("reason"));
    assertNull(out.get("resolve"));
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void invalidInputReturnsInvalidReason() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(12);
    detail.setDeleted(0);
    detail.setType("single");
    detail.setContent("只有题干没有任何选项");
    detail.setAnswer("A");
    when(examDetailMapper.selectById(12)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");

    Map<String, Object> out = service.ensureResolve(12);

    assertEquals("empty", out.get("source"));
    assertEquals("invalid_input", out.get("reason"));
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void missingQuestionThrows() {
    assertThrows(AppSmsException.class, () -> service.ensureResolve(999));
  }

  @Test
  void llmFailureWritesRejectedDraftAndCooldownBlocksRetry() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any()))
        .thenThrow(new RuntimeException("provider down"));

    Map<String, Object> first = service.ensureResolve(11);

    assertEquals("empty", first.get("source"));
    assertEquals("llm_failed", first.get("reason"));
    assertEquals(1, drafts.size());
    assertEquals("rejected", drafts.get(0).getStatus());
    assertTrue(drafts.get(0).getPrompt().startsWith("question_resolve_v2.1_rejected#"));
    // 冷却 key 已写入当前 sourceHash
    assertTrue(failStore.containsKey("question_resolve:fail:11"));

    Map<String, Object> second = service.ensureResolve(11);

    assertEquals("empty", second.get("source"));
    assertEquals("cooldown", second.get("reason"));
    verify(llmClient, times(1)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void lockContentionReturnsGeneratingWithoutLlm() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("empty", out.get("source"));
    assertEquals("generating", out.get("reason"));
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void staleDraftHashTriggersRegeneration() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");
    String firstText = "【考点】电气安全\n【正确】答案：A。旧解析说明安全电压为三十六伏，现场作业必须验电挂牌。"
        + "两人互保并完成记录，严禁单人操作。\n【干扰】选 B 错在把市电当安全电压。\n【记法】旧口诀记心间。";
    String secondText = "【考点】电气安全\n【正确】答案：B。新解析说明规程答案已变更为 B，现场按新规程执行。"
        + "两人互保并完成记录，严禁单人操作。\n【干扰】选 A 错在沿用旧规程。\n【记法】新口诀记心间。";
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(firstText, secondText);

    assertEquals("ai", service.ensureResolve(11).get("source"));

    // 题目答案变更 → sourceHash 变化 → 旧 draft 不使用，重新生成
    detail.setAnswer("B");
    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("ai", out.get("source"));
    assertEquals(secondText.trim(), out.get("resolve"));
    verify(llmClient, times(2)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void peekNeverReadsShadowOrRejectedAndNeverCallsLlm() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(resolveText());
    service.ensureResolve(11);
    assertEquals("shadow", drafts.get(0).getStatus());

    Map<String, Object> peeked = service.peekResolve(11);

    assertEquals("empty", peeked.get("source"));
    verify(llmClient, times(1)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void peekReadsGeneratedDraftWithMatchingHash() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(resolveText());
    service.ensureResolve(11);

    Map<String, Object> peeked = service.peekResolve(11);

    assertEquals("draft", peeked.get("source"));
    assertEquals(resolveText().trim(), peeked.get("resolve"));
    verify(llmClient, times(1)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void appEntryRejectsAnonymous() {
    SecurityContextHolder.clearContext();
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);

    AppSmsException ex = assertThrows(AppSmsException.class, () -> service.ensureResolveForApp(11));
    assertEquals("请先登录", ex.getMessage());
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void appEntryRateLimitedReturnsRateLimitedReason() {
    loginUser(501L);
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "上限"))
        .when(rateLimitService).acquire(eq(10159L), eq(501L));

    Map<String, Object> out = service.ensureResolveForApp(11);

    assertEquals("empty", out.get("source"));
    assertEquals("rate_limited", out.get("reason"));
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void appEntryCountsRateLimitOnlyBeforeRealLlm() {
    loginUser(502L);
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(resolveText());

    service.ensureResolveForApp(11);
    service.ensureResolveForApp(11);

    verify(rateLimitService, times(1)).acquire(eq(10159L), eq(502L));
    verify(llmClient, times(1)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void qualityRejectionWritesNoResolveAndCoolsDown() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any()))
        .thenReturn("完全没有任何小节结构的随口输出，不符合四行骨架要求。");

    Map<String, Object> first = service.ensureResolve(11);

    assertEquals("empty", first.get("source"));
    assertEquals("quality_rejected", first.get("reason"));
    // 质量拒绝：不写 resolve、写 rejected draft、进冷却、释放锁
    assertTrue(resolveUpdateWrappers.isEmpty());
    assertEquals(1, drafts.size());
    assertEquals("rejected", drafts.get(0).getStatus());
    assertTrue(drafts.get(0).getPrompt().startsWith("question_resolve_v2.1_rejected#"));
    assertTrue(failStore.containsKey("question_resolve:fail:11"));
    verify(redisTemplate, times(1))
        .execute(any(DefaultRedisScript.class), anyList(), any());

    Map<String, Object> second = service.ensureResolve(11);
    assertEquals("cooldown", second.get("reason"));
    verify(llmClient, times(1)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void officialOnlyKnowledgeSkipsLlmWithoutRejectedOrCooldown() {
    ExamExamDetailEntity detail = singleDetail();
    detail.setKnowledgeId(9);
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    ExamKnowledgeStructureEntity knowledge = new ExamKnowledgeStructureEntity();
    knowledge.setTitle("电工安全规程");
    when(knowledgeMapper.selectById(9)).thenReturn(knowledge);
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("empty", out.get("source"));
    assertEquals("official_only", out.get("reason"));
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
    assertTrue(drafts.isEmpty());
    assertTrue(failStore.isEmpty());
  }

  @Test
  void missingModelConfigGoesToRejectedDraftAndCooldown() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    when(modelConfigService.findByModelCode(10159L, "deepseek-review")).thenReturn(null);

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("empty", out.get("source"));
    assertEquals("llm_failed", out.get("reason"));
    assertEquals(1, drafts.size());
    assertEquals("rejected", drafts.get(0).getStatus());
    assertTrue(failStore.containsKey("question_resolve:fail:11"));
  }

  @Test
  void conditionalBackfillSkipsWhenHumanResolveExists() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");
    doAnswer(inv -> {
      resolveUpdateWrappers.add(inv.getArgument(1));
      return 0;
    }).when(examDetailMapper).update(any(), any());
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(resolveText());

    Map<String, Object> out = service.ensureResolve(11);

    // 影响 0 行 = 生成期间管理员已手工填写：AI 结果不覆盖，但 draft 仍留痕
    assertEquals("ai", out.get("source"));
    assertEquals(1, resolveUpdateWrappers.size());
    assertEquals(1, drafts.size());
    assertEquals("generated", drafts.get(0).getStatus());
    // 未回写不 evict：缓存里的人工解析仍然有效
    verify(reviewQuestionQueryService, never()).evictQuestionDetail(anyLong());
  }

  @Test
  void hashChangeRetriesImmediatelyAfterQualityRejection() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("question_resolve_generation_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any()))
        .thenReturn("没有小节结构的坏输出", resolveText());

    assertEquals("quality_rejected", service.ensureResolve(11).get("reason"));
    assertEquals("cooldown", service.ensureResolve(11).get("reason"));

    // 题目变更 → sourceHash 变化 → 立即重试
    detail.setContent("安全电压等级是多少\nA. 36\nB. 220");
    Map<String, Object> third = service.ensureResolve(11);
    assertEquals("shadow", third.get("source"));
    verify(llmClient, times(2)).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  /** 从 LambdaUpdateWrapper 提取 SET 的 resolve 值（先触发 MP3.4.1 SET 参数惰性合并）。 */
  private String resolveSetValue(Object wrapper) {
    if (wrapper instanceof com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<?> w) {
      w.getSqlSet();
      return w.getParamNameValuePairs().values().stream()
          .filter(String.class::isInstance).map(String.class::cast)
          .filter(s -> s.length() > 100)
          .findFirst().orElse(null);
    }
    return null;
  }

  @Test
  void modelCodeFallsBackToSharedKeyWhenResolveKeyAbsent() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    // setUp 已置 ai_review_model_code=deepseek-review；不配置新键
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(resolveText());

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("ai", out.get("source"));
    verify(modelConfigService).findByModelCode(10159L, "deepseek-review");
    assertEquals("deepseek-review", drafts.get(0).getModelName());
  }

  @Test
  void resolveModelKeyTakesPrecedenceAndFlowsToDraft() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("ai_review_model_code", "deepseek-review");
    dict("question_resolve_model_code", "theory-gen-glm");
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");
    ExamFaqModelConfigEntity glmConfig = new ExamFaqModelConfigEntity();
    glmConfig.setBaseUrl("https://open.bigmodel.cn/api/paas");
    glmConfig.setApiKey("k");
    glmConfig.setModelId("glm-5.3-flash");
    glmConfig.setThinking("enabled");
    when(modelConfigService.findByModelCode(10159L, "theory-gen-glm")).thenReturn(glmConfig);
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(resolveText());

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("ai", out.get("source"));
    // 模型配置查询与 draft.modelName 都用新键
    verify(modelConfigService).findByModelCode(10159L, "theory-gen-glm");
    assertEquals("theory-gen-glm", drafts.get(0).getModelName());
  }

  @Test
  void missingConfigForNewKeyFailsWithoutFallingBack() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dict("ai_review_model_code", "deepseek-review");
    dict("question_resolve_model_code", "nonexistent-model");
    dict("question_resolve_generation_enabled", "1");
    // findByModelCode 对 nonexistent-model 未打桩 → 返回 null

    Map<String, Object> out = service.ensureResolve(11);

    assertEquals("empty", out.get("source"));
    assertEquals("llm_failed", out.get("reason"));
    // 不回退旧模型：deepseek-review 的配置查询零调用、LLM 零调用
    verify(modelConfigService, never()).findByModelCode(10159L, "deepseek-review");
    verify(llmClient, never()).chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), any(), any(), anyBoolean(), any());
  }

  @Test
  void defaultModelUsedWhenBothKeysAbsent() {
    ExamExamDetailEntity detail = singleDetail();
    when(examDetailMapper.selectById(11)).thenReturn(detail);
    dictValues.remove("ai_review_model_code");
    dict("question_resolve_generation_enabled", "1");
    dict("question_resolve_writeback_enabled", "1");
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn(resolveText());

    service.ensureResolve(11);

    verify(modelConfigService).findByModelCode(10159L, "deepseek-review");
    assertEquals("deepseek-review", drafts.get(0).getModelName());
  }

  private void dict(String code, String value) {
    dictValues.put(code, value);
  }

  private void loginUser(Long userId) {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        userId, 10159L, "student", "学员", "student", List.of(), List.of(), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private ExamExamDetailEntity singleDetail() {
    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(11);
    detail.setDeleted(0);
    detail.setType("single");
    detail.setContent("安全电压是多少伏\nA. 36\nB. 220");
    detail.setAnswer("A");
    return detail;
  }

  private String resolveText() {
    return "【考点】安全电压\n【正确】答案：A。规程规定安全电压为 36 伏。\n"
        + "【干扰】220 伏是市电，不是安全电压。\n【记法】安全电压三十六。";
  }

  /** 超过 250 字的解析，验证回写无截断（§5.8 删除 RESOLVE_DB_MAX=250）。 */
  private String longResolveText() {
    StringBuilder sb = new StringBuilder("【考点】安全电压\n【正确】答案：A。规程规定安全电压为 36 伏。");
    while (sb.length() <= 300) {
      sb.append("现场作业必须使用三十六伏及以下电压，潮湿环境要更低，操作前必须验电并挂警示牌。");
    }
    sb.append("\n【干扰】220 伏是市电。\n【记法】安全电压三十六。");
    return sb.toString();
  }
}
