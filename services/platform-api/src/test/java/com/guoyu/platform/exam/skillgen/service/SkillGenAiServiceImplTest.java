package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/**
 * 操作技能生成 AI 服务单测（输出零信任；不依赖真实模型）。
 *
 * <p>单题拆分（2026-10-29）：T1~T10 对应用户方案 §5.1 用例表。
 */
class SkillGenAiServiceImplTest {

  private final SkillGenAiServiceImpl service =
      new SkillGenAiServiceImpl(null, new ObjectMapper(), new SkillQuestionValidator());

  private static final String ONE = """
      {"name":"联机启动集控室监控设备","durationMinutes":20,"examForm":"实操",
       "sitePrepare":{"note":"","equipment":[{"name":"主机","spec":"x","unit":"套","qty":"1","remark":""}],"faultSetup":""},
       "examineePrepare":"签字笔","requirements":"完成联机启动",
       "negatives":"若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违规",
       "scoring":[{"item":"a","score":"50","scoreDesc":"x","maxDeduct":"50","step":"10"},
                  {"item":"b","score":"50","scoreDesc":"y","maxDeduct":"50","step":"10"}]}""";

  /** 合法单题（满足 SkillQuestionValidator 全规则：五要素/保底/步长5/扣分项数/合计100） */
  private static String q(String name, int minutes) {
    return q(name, minutes, 100);
  }

  /** 合法单题，可指定配分合计（≠100 时被 validator 拦截，用于校验重试用例） */
  private static String q(String name, int minutes, int totalScore) {
    int first = 50;
    int second = 30;
    int third = totalScore - first - second;
    if (third <= 0) {
      throw new IllegalArgumentException("totalScore 需大于 80");
    }
    return """
        {"name":"%s","durationMinutes":%d,"examForm":"实操",
         "sitePrepare":{"equipment":[{"name":"考核设备","unit":"台","qty":"1"}]},
         "examineePrepare":"本人身份证×1、准考证×1、黑色签字笔×1",
         "requirements":"请完成%s规定的考核操作",
         "negatives":"若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违反安全操作规程",
         "scoring":[{"item":"要素一","score":"%d","scoreDesc":"%s","maxDeduct":"%d","step":"5"},
                    {"item":"要素二","score":"%d","scoreDesc":"%s","maxDeduct":"%d","step":"5"},
                    {"item":"要素三","score":"%d","scoreDesc":"%s","maxDeduct":"%d","step":"5"}]}"""
        .formatted(name, minutes, name,
            first, desc(first), first,
            second, desc(second), second,
            third, desc(third), third);
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

  private FaqLlmClient clientReturning(String... contents) {
    FaqLlmClient client = mock(FaqLlmClient.class);
    var stub = when(client.chatCompletionDetailed(anyString(), anyString(), anyString(), any(), any(),
        anyInt(), any(), any(), anyBoolean(), any(), anyInt()));
    if (contents.length == 1) {
      stub.thenReturn(result(contents[0]));
    } else {
      LlmChatCompletionResult[] results = new LlmChatCompletionResult[contents.length];
      for (int i = 0; i < contents.length; i++) {
        results[i] = result(contents[i]);
      }
      stub.thenReturn(results[0], java.util.Arrays.copyOfRange(results, 1, results.length));
    }
    return client;
  }

  private SkillGenAiService.SkillGenGeneration generate(FaqLlmClient client, int expected) {
    return new SkillGenAiServiceImpl(client, new ObjectMapper(), new SkillQuestionValidator())
        .generateForPointWithRepair(1L, "https://x", "k", "glm-5.3-flash", "enabled", "low",
            16384, "sys", "考评点代码：A-A-001\n", expected);
  }

  // ---------- 数组解析（保留兼容，仍被 parseQuestions 覆盖） ----------

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
    // 数组模式不得裸包单对象（单题拆分后由 parseSingleQuestion 承接该形态）
    assertThrows(BusinessException.class, () -> service.parseQuestions(ONE));
  }

  // ---------- T2/T3：单题解析 ----------

  /** T2：单对象（主形态）/ 1 元素数组 / 白名单包装对象 三种形态都解析成功 */
  @Test
  void parseSingle_acceptsObjectOneElementArrayAndWrapper() {
    assertEquals("联机启动集控室监控设备", service.parseSingleQuestion(ONE).getName());
    assertEquals("联机启动集控室监控设备",
        service.parseSingleQuestion("[" + ONE + "]").getName());
    assertEquals("联机启动集控室监控设备",
        service.parseSingleQuestion("{\"questions\":[" + ONE + "]}").getName());
    assertEquals("联机启动集控室监控设备",
        service.parseSingleQuestion("```json\n" + ONE + "\n```").getName());
  }

  /** T3：空内容 / 多元素数组 / 空数组 / 非 JSON 一律抛 BusinessException */
  @Test
  void parseSingle_rejectsEmptyMultiElementAndNonJson() {
    assertThrows(BusinessException.class, () -> service.parseSingleQuestion(""));
    assertThrows(BusinessException.class, () -> service.parseSingleQuestion("   "));
    assertThrows(BusinessException.class, () -> service.parseSingleQuestion("这不是 JSON"));
    assertThrows(BusinessException.class,
        () -> service.parseSingleQuestion("[" + q("题一", 20) + "," + q("题二", 30) + "]"));
    assertThrows(BusinessException.class, () -> service.parseSingleQuestion("[]"));
    assertThrows(BusinessException.class,
        () -> service.parseSingleQuestion("{\"questions\":[]}"));
  }

  // ---------- T1：5 次单题调用 ----------

  /** T1：5 次各返回合法单题对象 → 5 题、attempts=5、usage 为 5 次累计 */
  @Test
  void single_fiveSequentialItemsSucceed() {
    FaqLlmClient client = clientReturning(q("题一", 20), q("题二", 25), q("题三", 30),
        q("题四", 35), q("题五", 40));

    SkillGenAiService.SkillGenGeneration gen = generate(client, 5);

    assertEquals(5, gen.items().size());
    assertEquals(5, gen.attempts(), "无重试时 attempts = 总调用次数 = 题数");
    assertEquals(500, gen.promptTokens(), "usage 应按 5 次调用累计");
    assertEquals(1000, gen.completionTokens());
    assertEquals(1500, gen.totalTokens());
    assertTrue(gen.content().contains("题五"), "content 应为末次成功输出（审计用途）");
    verify(client, times(5)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), any(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
  }

  // ---------- T4/T5/T6：单题重试 ----------

  /** T4：第 2 题首次返回非法 JSON → 重试成功；总调用 6 次，最终 5 题 */
  @Test
  void retry_invalidJsonOnSecondQuestion() {
    FaqLlmClient client = clientReturning(q("题一", 20), "这不是 JSON", q("题二", 25),
        q("题三", 30), q("题四", 35), q("题五", 40));

    SkillGenAiService.SkillGenGeneration gen = generate(client, 5);

    assertEquals(5, gen.items().size());
    assertEquals(6, gen.attempts(), "第 2 题重试一次 → 5+1 次调用");
    assertEquals("题二", gen.items().get(1).getName());
  }

  /** T5：第 3 题配分合计≠100 被 validator 拦截 → 重试该题成功 */
  @Test
  void retry_scoreSumNotHundredThenSucceeds() {
    FaqLlmClient client = clientReturning(q("题一", 20), q("题二", 25), q("题三", 30, 90),
        q("题三", 30), q("题四", 35), q("题五", 40));

    SkillGenAiService.SkillGenGeneration gen = generate(client, 5);

    assertEquals(5, gen.items().size());
    assertEquals(6, gen.attempts(), "校验失败必须重试该题（不是整体报废）");
    ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
    verify(client, times(6)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), prompts.capture(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
    String retryPrompt = prompts.getAllValues().get(3);
    assertTrue(retryPrompt.contains("上次输出有误"), "重试 prompt 必须说明上次失败原因");
    assertTrue(retryPrompt.contains("配分"), "重试原因应包含校验器给出的配分错误");
  }

  /** T6：第 2 题与第 1 题同名（查重命中）→ 重试该题；重试 prompt 含已生成题名 */
  @Test
  void retry_duplicateNameThenSucceeds() {
    FaqLlmClient client = clientReturning(q("题一", 20), q("题一", 25), q("题二", 25),
        q("题三", 30), q("题四", 35), q("题五", 40));

    SkillGenAiService.SkillGenGeneration gen = generate(client, 5);

    assertEquals(5, gen.items().size());
    assertEquals(6, gen.attempts());
    assertEquals(5, gen.items().stream().map(SkillQuestionItem::getName).distinct().count(),
        "最终 5 题名称必须互不重复");
    ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
    verify(client, times(6)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), prompts.capture(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
    String retryPrompt = prompts.getAllValues().get(2);
    assertTrue(retryPrompt.contains("已生成试题名称（禁止重复或同义改述）：题一"),
        "查重重试必须再次强调已生成题名");
    assertTrue(retryPrompt.contains("同点内试题名称重复"), "重试原因应指明名称重复");
  }

  // ---------- T7：prompt 逐次核对 ----------

  /** T7：第 1 次调用不含「已生成」，第 3 次调用含前 2 题名称；均带第 n 题指令与单对象契约 */
  @Test
  void prompt_carriesGeneratedNamesPerQuestion() {
    FaqLlmClient client = clientReturning(q("题一", 20), q("题二", 25), q("题三", 30),
        q("题四", 35), q("题五", 40));

    generate(client, 5);

    ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
    verify(client, times(5)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), prompts.capture(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
    List<String> all = prompts.getAllValues();
    assertFalse(all.get(0).contains("已生成试题名称"), "首题不得出现已生成题名");
    assertTrue(all.get(0).contains("请为该考评点生成第 1 道完整试题（共 5 道"),
        "首题 prompt 应带第 1 题指令");
    assertTrue(all.get(2).contains("请为该考评点生成第 3 道完整试题（共 5 道"));
    assertTrue(all.get(2).contains("已生成试题名称（禁止重复或同义改述）：题一、题二"),
        "第 3 题 prompt 必须携带前 2 题名称");
    assertTrue(all.get(4).contains("题一、题二、题三、题四"));
    for (String p : all) {
      assertTrue(p.contains("只输出 1 个 JSON 对象"), "每次调用都必须声明单对象契约");
      assertFalse(p.contains("恰好 5 个元素"), "单题 prompt 不得残留数组契约");
      assertTrue(p.contains("考评点代码：A-A-001"), "基础信息必须原样带入每题 prompt");
    }
  }

  // ---------- T8/T9：失败与截断 ----------

  /** T8：某题连续 3 次失败 → BusinessException，message 含「第 N 题」与「已调用 3 次」 */
  @Test
  void retry_threeAttemptsFailThrowsWithQuestionIndex() {
    FaqLlmClient client = clientReturning(q("题一", 20), q("题二", 25),
        "这不是 JSON", "这不是 JSON", "这不是 JSON");

    BusinessException ex = assertThrows(BusinessException.class, () -> generate(client, 5));

    assertTrue(ex.getMessage().contains("第 3 题"), "错误信息必须指出第几题");
    assertTrue(ex.getMessage().contains("已调用 3 次"), "错误信息应含该题调用次数");
    verify(client, times(5)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), any(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
  }

  /** T9：重试 prompt 回喂失败输出且超 4000 字符截断 */
  @Test
  void retry_echoTruncatedForOversizedOutput() {
    FaqLlmClient client = clientReturning("x".repeat(6000), q("题一", 20), q("题二", 25),
        q("题三", 30), q("题四", 35), q("题五", 40));

    generate(client, 5);

    ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
    verify(client, times(6)).chatCompletionDetailed(anyString(), anyString(), anyString(),
        any(), prompts.capture(), anyInt(), any(), any(), anyBoolean(), any(), anyInt());
    String retryPrompt = prompts.getAllValues().get(1);
    assertTrue(retryPrompt.contains("…(截断)"), "超长输出回喂必须截断");
    assertTrue(retryPrompt.length() < 6000, "重试 prompt 体积必须受控");
  }

  // ---------- T10：system prompt 单对象契约 ----------

  @Test
  void prompt_systemDeclaresSingleObjectContract() {
    String system = SkillGenPromptBuilder.buildSystemPrompt(2);
    assertTrue(system.contains("只输出 1 个 JSON 对象（一道完整试题的本体）"));
    assertTrue(system.contains("不要输出 JSON 数组"));
    // 示例必须是单个对象（不含数组包裹），否则模型会模仿数组形态
    String example = system.substring(system.indexOf("输出示例"));
    assertEquals(1, example.split("\"durationMinutes\"", -1).length - 1,
        "示例只能有 1 个试题对象");
    assertFalse(example.contains("\"questions\""), "示例不得出现包装字段");
    assertFalse(example.strip().startsWith("["), "示例不得以数组开头");
  }

  @Test
  void prompt_containsKeyRules() {
    String system = SkillGenPromptBuilder.buildSystemPrompt(2);
    assertTrue(system.contains("配分合计必须等于 100"));
    assertTrue(system.contains("终止其考试"));
    assertTrue(system.contains("不得同义改述"));
    assertTrue(system.contains("二级/技师"));
    assertEquals(5, SkillGenPromptBuilder.fixedQuota());
    // 命题硬性要求 1~8 条必须原样保留（本方案只改输出契约与示例）
    for (String rule : List.of("1. 每道试题对应一个考评点", "4. 评分记录表规则", "7. 同一考评点下的多道试题",
        "8. 试题内容正确无争议")) {
      assertTrue(system.contains(rule), "命题规则不得被改动：" + rule);
    }
  }

  /** 数组提示词构造器保留（@Deprecated），不得被单题链路使用 */
  @Test
  @SuppressWarnings("deprecation")
  void prompt_legacyArrayBuilderStillAvailable() {
    var point = new com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity();
    point.setFullCode("A-A-001");
    point.setName("考评点");
    String legacy = SkillGenPromptBuilder.buildUserPrompt(point, null, null, 5);
    assertTrue(legacy.contains("恰好 5 个元素"));
  }
}
