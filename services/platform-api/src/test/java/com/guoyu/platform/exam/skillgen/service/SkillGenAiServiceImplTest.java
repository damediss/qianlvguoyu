package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 操作技能生成 AI 服务解析单测（输出零信任；不依赖真实模型） */
class SkillGenAiServiceImplTest {

  private final SkillGenAiServiceImpl service =
      new SkillGenAiServiceImpl(null, new ObjectMapper());

  private static final String ONE = """
      {"name":"联机启动集控室监控设备","durationMinutes":20,"examForm":"实操",
       "sitePrepare":{"note":"","equipment":[{"name":"主机","spec":"x","unit":"套","qty":"1","remark":""}],"faultSetup":""},
       "examineePrepare":"签字笔","requirements":"完成联机启动",
       "negatives":"若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违规",
       "scoring":[{"item":"a","score":"50","scoreDesc":"x","maxDeduct":"50","step":"10"},
                  {"item":"b","score":"50","scoreDesc":"y","maxDeduct":"50","step":"10"}]}""";

  /** 合法单题（满足 SkillQuestionValidator 全规则：五要素/保底/步长5/扣分项数/合计100） */
  private static String q(String name, int minutes) {
    return """
        {"name":"%s","durationMinutes":%d,"examForm":"实操",
         "sitePrepare":{"equipment":[{"name":"考核设备","unit":"台","qty":"1"}]},
         "examineePrepare":"本人身份证×1、准考证×1、黑色签字笔×1",
         "requirements":"请完成%s规定的考核操作",
         "negatives":"若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违反安全操作规程",
         "scoring":[{"item":"要素一","score":"50","scoreDesc":"%s","maxDeduct":"50","step":"5"},
                    {"item":"要素二","score":"30","scoreDesc":"%s","maxDeduct":"30","step":"5"},
                    {"item":"要素三","score":"20","scoreDesc":"%s","maxDeduct":"20","step":"5"}]}"""
        .formatted(name, minutes, name, desc(50), desc(30), desc(20));
  }

  /** 评分标准按步长列扣分项：「扣5分」出现次数 = 最大扣分÷5（V10 口径） */
  private static String desc(int maxDeduct) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < maxDeduct / 5; i++) {
      sb.append("考核点未达标扣5分；");
    }
    return sb.toString();
  }

  private static String array5() {
    return "[" + q("题一", 20) + "," + q("题二", 30) + "," + q("题三", 40)
        + "," + q("题四", 50) + "," + q("题五", 60) + "]";
  }

  private static LlmChatCompletionResult result(String content) {
    return new LlmChatCompletionResult(content, 100, 200, 300, 0);
  }

  @Test
  void parse_bareArray() {
    List<SkillQuestionItem> items = service.parseQuestions("[" + ONE + "]");
    assertEquals(1, items.size());
    assertEquals("联机启动集控室监控设备", items.get(0).getName());
    assertEquals(20, items.get(0).getDurationMinutes());
    assertEquals(2, items.get(0).getScoring().size());
  }

  @Test
  void parse_wrappedObjectAndFences() {
    String wrapped = "```json\n{\"questions\":[" + ONE + "]}\n```";
    List<SkillQuestionItem> items = service.parseQuestions(wrapped);
    assertEquals(1, items.size());
  }

  @Test
  void parse_badJsonThrows() {
    assertThrows(BusinessException.class, () -> service.parseQuestions("这不是 JSON"));
    assertThrows(BusinessException.class, () -> service.parseQuestions(""));
    assertThrows(BusinessException.class, () -> service.parseQuestions("[]"));
  }

  @Test
  void parse_unknownFieldsIgnored() {
    String extra = ONE.replace("\"examForm\":\"实操\"", "\"examForm\":\"实操\",\"extra\":\"忽略\"");
    assertEquals(1, service.parseQuestions("[" + extra + "]").size());
  }

  @Test
  void parse_snakeCaseAliasesAccepted() {
    String snake = ONE.replace("\"durationMinutes\"", "\"duration_minutes\"")
        .replace("\"examForm\"", "\"exam_form\"")
        .replace("\"sitePrepare\"", "\"site_prepare\"")
        .replace("\"faultSetup\"", "\"fault_setup\"")
        .replace("\"examineePrepare\"", "\"examinee_prepare\"");
    List<SkillQuestionItem> items = service.parseQuestions("[" + snake + "]");
    assertEquals(20, items.get(0).getDurationMinutes());
    assertEquals("实操", items.get(0).getExamForm());
    assertEquals("签字笔", items.get(0).getExamineePrepare());
  }

  @Test
  void prompt_containsKeyRules() {
    String system = SkillGenPromptBuilder.buildSystemPrompt(2);
    assertTrue(system.contains("配分合计必须等于 100"));
    assertTrue(system.contains("终止其考试"));
    assertTrue(system.contains("不得同义改述"));
    assertTrue(system.contains("二级/技师"));
    assertEquals(5, SkillGenPromptBuilder.fixedQuota());
  }

  // ---------- GLM 数组塌缩对策（2026-10-29）：白名单提取 + 修正重试 ----------

  @Test
  void parse_objectWrappedWhitelistKeys() {
    for (String key : List.of("questions", "data", "items", "result")) {
      String wrapped = "{\"" + key + "\":[" + ONE + "]}";
      assertEquals(1, service.parseQuestions(wrapped).size(),
          "白名单键 " + key + " 应可提取数组");
    }
  }

  @Test
  void parse_whitelistKeyNotArrayThrows() {
    assertThrows(BusinessException.class,
        () -> service.parseQuestions("{\"questions\":{\"name\":\"x\"}}"));
    // 对象里带无关数组字段不得误提取（键不在白名单）
    assertThrows(BusinessException.class,
        () -> service.parseQuestions("{\"foo\":[" + ONE + "]}"));
  }

  @Test
  void parse_bareSingleObjectThrows() {
    // GLM 塌缩形态：裸单对象不裸包、不静默放行，必须抛错触发修正重试
    assertThrows(BusinessException.class, () -> service.parseQuestions(ONE));
  }

  @Test
  void repair_bareObjectThenArraySucceeds() {
    FaqLlmClient client = mock(FaqLlmClient.class);
    SkillGenAiServiceImpl svc = new SkillGenAiServiceImpl(client, new ObjectMapper());
    when(client.chatCompletionDetailed(anyString(), anyString(), anyString(), any(), any(),
        anyInt(), any(), any(), anyBoolean(), any(), anyInt()))
        .thenReturn(result(ONE), result(array5()));

    SkillGenAiService.SkillGenGeneration gen = svc.generateForPointWithRepair(
        1L, "https://x", "k", "glm-5.3-flash", "enabled", "high", 16384, "sys", "user", 5);

    assertEquals(5, gen.items().size());
    assertEquals(2, gen.attempts(), "首次塌缩后应修正重试一次成功");
    assertEquals(600, gen.totalTokens(), "usage 应累计全部尝试：300+300");
    ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
    verify(client, times(2)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), cap.capture(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
    String secondPrompt = cap.getAllValues().get(1);
    assertTrue(secondPrompt.contains("上次输出有误"), "修正 prompt 应说明上次失败原因");
    assertTrue(secondPrompt.contains("上次输出如下"), "修正 prompt 应回喂失败输出");
    assertTrue(secondPrompt.contains("恰好 5 个元素"), "修正 prompt 应重申输出契约");
  }

  @Test
  void repair_countMismatchThenArraySucceeds() {
    FaqLlmClient client = mock(FaqLlmClient.class);
    SkillGenAiServiceImpl svc = new SkillGenAiServiceImpl(client, new ObjectMapper());
    when(client.chatCompletionDetailed(anyString(), anyString(), anyString(), any(), any(),
        anyInt(), any(), any(), anyBoolean(), any(), anyInt()))
        .thenReturn(result("[" + q("题一", 20) + "," + q("题二", 30) + "," + q("题三", 40) + "]"),
            result(array5()));

    SkillGenAiService.SkillGenGeneration gen = svc.generateForPointWithRepair(
        1L, "https://x", "k", "glm-5.3-flash", "enabled", "high", 16384, "sys", "user", 5);

    assertEquals(5, gen.items().size());
    assertEquals(2, gen.attempts(), "数量不符（3≠5）也应触发修正重试");
  }

  @Test
  void repair_allAttemptsFailThrowsWithAttemptCount() {
    FaqLlmClient client = mock(FaqLlmClient.class);
    SkillGenAiServiceImpl svc = new SkillGenAiServiceImpl(client, new ObjectMapper());
    when(client.chatCompletionDetailed(anyString(), anyString(), anyString(), any(), any(),
        anyInt(), any(), any(), anyBoolean(), any(), anyInt()))
        .thenReturn(result(ONE), result(ONE), result(ONE));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> svc.generateForPointWithRepair(1L, "https://x", "k", "glm-5.3-flash",
            "enabled", "high", 16384, "sys", "user", 5));

    assertTrue(ex.getMessage().contains("已调用 3 次"), "错误信息应含调用次数便于排障");
    verify(client, times(3)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), any(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
  }

  @Test
  void repair_firstSuccessNoRetry() {
    FaqLlmClient client = mock(FaqLlmClient.class);
    SkillGenAiServiceImpl svc = new SkillGenAiServiceImpl(client, new ObjectMapper());
    when(client.chatCompletionDetailed(anyString(), anyString(), anyString(), any(), any(),
        anyInt(), any(), any(), anyBoolean(), any(), anyInt()))
        .thenReturn(result(array5()));

    SkillGenAiService.SkillGenGeneration gen = svc.generateForPointWithRepair(
        1L, "https://x", "k", "glm-5.3-flash", "enabled", "high", 16384, "sys", "user", 5);

    assertEquals(1, gen.attempts());
    assertEquals(5, gen.items().size());
    verify(client, times(1)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), any(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
  }

  @Test
  void repair_echoTruncatedForOversizedOutput() {
    FaqLlmClient client = mock(FaqLlmClient.class);
    SkillGenAiServiceImpl svc = new SkillGenAiServiceImpl(client, new ObjectMapper());
    String huge = ONE + "x".repeat(6000);
    when(client.chatCompletionDetailed(anyString(), anyString(), anyString(), any(), any(),
        anyInt(), any(), any(), anyBoolean(), any(), anyInt()))
        .thenReturn(result(huge), result(array5()));

    svc.generateForPointWithRepair(1L, "https://x", "k", "glm-5.3-flash", "enabled", "high",
        16384, "sys", "user", 5);

    ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
    verify(client, times(2)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), cap.capture(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
    assertTrue(cap.getAllValues().get(1).contains("…(截断)"), "超长输出回喂必须截断");
    assertTrue(cap.getAllValues().get(1).length() < 6000, "修正 prompt 体积必须受控");
  }

  @Test
  void prompt_exampleHasTwoElements() {
    String system = SkillGenPromptBuilder.buildSystemPrompt(2);
    assertTrue(system.contains("实际输出的元素个数必须与题目要求一致"));
    String example = system.substring(system.indexOf("输出示例"));
    // 每题对象含唯一键 durationMinutes（设备名等不含），据此数示例元素个数
    assertEquals(2, example.split("\"durationMinutes\"", -1).length - 1,
        "示例必须是 2 个元素（单元素示例诱发模型模仿只输出 1 题）");
  }
}
