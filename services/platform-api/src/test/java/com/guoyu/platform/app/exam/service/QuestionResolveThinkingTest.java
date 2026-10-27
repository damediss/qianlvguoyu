package com.guoyu.platform.app.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.exam.reviewsync.service.ReviewQuestionQueryService;
import com.guoyu.platform.exam.service.OfficialResolveResolver;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory;
import com.guoyu.platform.exam.service.QuestionResolveRateLimitService;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 题目解析 LLM 调用参数：thinking 必须取模型配置（禁用思考防思维链吃光 max_tokens）；
 * maxTokens=800（§5.6：关闭思考 800，开启 2400）。契约保持绿，输入夹具适配
 * 「单选题必须有选项」的新资格规则（§5.5）。
 */
class QuestionResolveThinkingTest {

  private final Map<String, String> dictValues = new HashMap<>();

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, DictEntity.class);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
    dictValues.clear();
  }

  @Test
  void thinkingAndMaxTokensComeFromConfig() {
    ExamExamDetailMapper examDetailMapper = mock(ExamExamDetailMapper.class);
    FollowTaskBankQuestionConverter converter = mock(FollowTaskBankQuestionConverter.class);
    FaqLlmClient llmClient = mock(FaqLlmClient.class);
    FaqModelConfigApplicationService modelConfigService = mock(FaqModelConfigApplicationService.class);
    DictMapper dictMapper = mock(DictMapper.class);
    AiGenerationDraftMapper draftMapper = mock(AiGenerationDraftMapper.class);
    ExamKnowledgeStructureMapper knowledgeMapper = mock(ExamKnowledgeStructureMapper.class);
    QuestionResolveRateLimitService rateLimitService = mock(QuestionResolveRateLimitService.class);
    ReviewQuestionQueryService reviewQuestionQueryService = mock(ReviewQuestionQueryService.class);
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    QuestionResolveInputFactory inputFactory = new QuestionResolveInputFactory(
        converter, knowledgeMapper, new ObjectMapper());
    OfficialResolveResolver resolver = new OfficialResolveResolver(draftMapper, inputFactory, new ObjectMapper());
    QuestionResolveAssistService service = new QuestionResolveAssistService(
        examDetailMapper, converter, llmClient, modelConfigService, dictMapper, draftMapper,
        resolver, inputFactory, rateLimitService, reviewQuestionQueryService, redisTemplate);

    ExamExamDetailEntity detail = new ExamExamDetailEntity();
    detail.setId(1);
    detail.setDeleted(0);
    detail.setType("single");
    detail.setContent("题干内容\nA. 选项甲\nB. 选项乙");
    detail.setAnswer("A");
    when(examDetailMapper.selectById(1)).thenReturn(detail);
    when(draftMapper.selectOne(any())).thenReturn(null);

    FollowTaskQuestionReq snap = new FollowTaskQuestionReq();
    snap.setStem("题干内容");
    snap.setAnswer("A");
    snap.setOptions(List.of(
        Map.of("key", "A", "text", "选项甲"), Map.of("key", "B", "text", "选项乙")));
    when(converter.convert(any(), eq(0))).thenReturn(snap);

    ExamFaqModelConfigEntity config = new ExamFaqModelConfigEntity();
    config.setBaseUrl("https://api.deepseek.com");
    config.setApiKey("k");
    config.setModelId("deepseek-flash");
    config.setThinking("disabled");
    config.setReasoningEffort("high");

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
    dictValues.put("ai_review_model_code", "deepseek-review");
    dictValues.put("question_resolve_generation_enabled", "1");
    dictValues.put("question_resolve_writeback_enabled", "1");
    when(modelConfigService.findByModelCode(10159L, "deepseek-review")).thenReturn(config);

    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(valueOps.get(anyString())).thenReturn(null);
    doReturn(1L).when(redisTemplate)
        .execute(any(org.springframework.data.redis.core.script.DefaultRedisScript.class),
            anyList(), any());

    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any()))
        .thenReturn("【考点】配料\n【正确】答案：A。选甲。因为甲符合配料规程的要求，"
            + "现场必须按此执行并如实记录。\n【干扰】乙是干扰项，不符合配比要求。\n【记法】甲对乙错。");

    TenantContextHolder.setTenantId(10159L);
    Map<String, Object> result = service.ensureResolve(1);
    assertEquals("ai", result.get("source"));

    verify(llmClient).chatCompletion(
        eq("https://api.deepseek.com"), eq("k"), eq("deepseek-flash"),
        anyString(), anyString(),
        eq(800), eq("disabled"), eq("high"), eq(false), eq(10159L));
  }
}
