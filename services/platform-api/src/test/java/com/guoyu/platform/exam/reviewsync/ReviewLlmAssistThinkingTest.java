package com.guoyu.platform.exam.reviewsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.reviewsync.service.ReviewLlmAssistService;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.system.mapper.DictMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 鼓励语 LLM 调用参数：thinking 必须取模型配置（禁用思考防思维链吃光 max_tokens），maxTokens=500 */
class ReviewLlmAssistThinkingTest {

  @Test
  void thinkingAndMaxTokensComeFromConfig() {
    FaqLlmClient llmClient = mock(FaqLlmClient.class);
    FaqModelConfigApplicationService modelConfigService = mock(FaqModelConfigApplicationService.class);
    DictMapper dictMapper = mock(DictMapper.class);
    AiGenerationDraftMapper draftMapper = mock(AiGenerationDraftMapper.class);
    ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    ReviewLlmAssistService service = new ReviewLlmAssistService(
        llmClient, modelConfigService, dictMapper, draftMapper, executor);

    ExamFaqModelConfigEntity config = new ExamFaqModelConfigEntity();
    config.setBaseUrl("https://api.deepseek.com");
    config.setApiKey("k");
    config.setModelId("deepseek-flash");
    config.setThinking("disabled");
    config.setReasoningEffort("high");

    when(dictMapper.selectList(any())).thenReturn(List.of());
    when(modelConfigService.findByModelCode(10159L, "deepseek-review")).thenReturn(config);
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), any(), any(), anyBoolean(), any())).thenReturn("加油，今天也要稳扎稳打！");

    String out = service.generateAndCache(99L, 1, 1, "重冶备料工", "三级", 7, 0.62, false, 10159L);
    assertEquals("加油，今天也要稳扎稳打！", out);

    verify(llmClient).chatCompletion(
        eq("https://api.deepseek.com"), eq("k"), eq("deepseek-flash"),
        anyString(), anyString(),
        eq(500), eq("disabled"), eq("high"), eq(false), eq(10159L));
  }
}
