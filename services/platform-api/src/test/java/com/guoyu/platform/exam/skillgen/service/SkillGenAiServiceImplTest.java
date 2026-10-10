package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import java.util.List;
import org.junit.jupiter.api.Test;

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
}
