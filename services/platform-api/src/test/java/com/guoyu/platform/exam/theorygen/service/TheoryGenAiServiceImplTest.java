package com.guoyu.platform.exam.theorygen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 理论生成 AI 服务：JSON 解析零信任单测（方案 GEN-5，桩响应不依赖真实模型） */
class TheoryGenAiServiceImplTest {

  private final FaqLlmClient llmClient = mock(FaqLlmClient.class);
  private final TheoryGenAiServiceImpl service = new TheoryGenAiServiceImpl(llmClient, new ObjectMapper());

  private static final String VALID_JSON = """
      [
        {"questionType":"single","typeCode":"B","difficulty":3,
         "content":"氧化铝的化学式是（ ）。",
         "options":[{"label":"A","content":"Al2O3"},{"label":"B","content":"Al(OH)3"},
                    {"label":"C","content":"AlCl3"},{"label":"D","content":"AlN"}],
         "answer":"A","analysis":"氧化铝为 Al2O3"},
        {"questionType":"judge","typeCode":"C","difficulty":2,
         "content":"拜耳法溶出过程在常温常压下进行。",
         "answer":"×","analysis":"高压溶出"}
      ]
      """;

  @Test
  void parseValidArray() {
    List<TheoryQuestionItem> items = service.parseQuestions(VALID_JSON);
    assertEquals(2, items.size());
    assertEquals("single", items.get(0).getQuestionType());
    assertEquals(4, items.get(0).getOptionList().size());
    assertEquals("×", items.get(1).getAnswer());
  }

  @Test
  void parseStripsMarkdownFence() {
    String fenced = "```json\n" + VALID_JSON + "\n```";
    assertEquals(2, service.parseQuestions(fenced).size());
  }

  @Test
  void parseAcceptsQuestionsWrapper() {
    String wrapped = "{\"questions\":" + VALID_JSON + "}";
    assertEquals(2, service.parseQuestions(wrapped).size());
  }

  @Test
  void parseRejectsGarbage() {
    assertThrows(BusinessException.class, () -> service.parseQuestions("这不是 JSON"));
    assertThrows(BusinessException.class, () -> service.parseQuestions(""));
    assertThrows(BusinessException.class, () -> service.parseQuestions("{\"questions\":\"not-array\"}"));
  }

  @Test
  void parseRejectsEmptyArray() {
    assertThrows(BusinessException.class, () -> service.parseQuestions("[]"));
  }

  @Test
  void generateForPointPassesThinkingSnapshot() {
    when(llmClient.chatCompletion(anyString(), anyString(), anyString(), anyString(),
        anyString(), anyInt(), eq("enabled"), eq("low"), eq(false), anyLong()))
        .thenReturn(VALID_JSON);
    String raw = service.generateForPoint(1L, "https://open.bigmodel.cn/api/paas/v4", "k",
        "GLM-5.3-Flash", "enabled", "low", 4096, "sys", "user");
    assertTrue(raw.contains("questionType"));
  }
}
