package com.guoyu.platform.exam.standard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.entity.ExamStandardTaskEntity;
import com.guoyu.platform.exam.standard.mapper.ExamStandardNodeMapper;
import com.guoyu.platform.exam.standard.mapper.ExamStandardTaskMapper;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 考评点名称形态修复（2026-09-11 需求方确认按《指引》第二章严格执行）：
 * ① 一条只考核一个主要内容（检出「与/和/及/、」并列）；② 无疑问句；③ ≤20 字；
 * 检出后按小节重命名一次（条数不变、原位更新），仍不合规保留原名并告警。
 */
class StandardNameShapeRepairTest {

  // ---------- 检出（纯函数） ----------

  @Test
  void detectsJoinedTwoContents() {
    assertNotNull(StandardLlmSummarizeService.nameShapeIssue("设备日常保养周期与要求"));
    assertNotNull(StandardLlmSummarizeService.nameShapeIssue("润滑油种类与选用"));
    assertNotNull(StandardLlmSummarizeService.nameShapeIssue("故障预防和改进"));
    assertNotNull(StandardLlmSummarizeService.nameShapeIssue("周期、要求"));
    // 复合标准术语也会命中（检出从严；是否保留由重命名链路判断，见 §改名 prompt）
    assertNotNull(StandardLlmSummarizeService.nameShapeIssue("公差与配合基本概念"));
  }

  @Test
  void detectsQuestionAndOverLength() {
    assertNotNull(StandardLlmSummarizeService.nameShapeIssue("什么是润滑油的选用要求？"));
    assertNotNull(StandardLlmSummarizeService.nameShapeIssue("一二三四五六七八九十一二三四五六七八九十一"));
    assertEquals("含疑问句", StandardLlmSummarizeService.nameShapeIssue("设备如何保养?"));
  }

  @Test
  void cleanNamesPass() {
    assertNull(StandardLlmSummarizeService.nameShapeIssue("设备日常维护保养内容"));
    assertNull(StandardLlmSummarizeService.nameShapeIssue("润滑油加注方法"));
    assertNull(StandardLlmSummarizeService.nameShapeIssue("重金属密度的测定原理"));
    assertNull(StandardLlmSummarizeService.nameShapeIssue(null));
  }

  /** 目标筛选：仅 LLM 产出且未审核通过的理论叶（人工/已审不动） */
  @Test
  void targetsOnlyLlmUnapprovedViolators() {
    ExamStandardNodeEntity llmBad = leaf(1L, "设备日常保养周期与要求", "llm", "pending");
    ExamStandardNodeEntity llmGood = leaf(2L, "设备日常维护保养内容", "llm", "pending");
    ExamStandardNodeEntity manualBad = leaf(3L, "种类与选用", "manual", "pending");
    ExamStandardNodeEntity approvedBad = leaf(4L, "周期与要求", "llm", "approved");
    List<ExamStandardNodeEntity> targets = StandardLlmSummarizeService
        .selectNameShapeTargets(List.of(llmBad, llmGood, manualBad, approvedBad));
    assertEquals(1, targets.size());
    assertEquals(1L, targets.get(0).getId());
  }

  // ---------- 端到端（mock LLM）：重命名一次 + 仍不合规告警 ----------

  @Test
  void renamesInPlaceWhenModelReturnsCompliantName() {
    Fixture f = newFixture(
        "{\"points\":[{\"title\":\"设备日常保养要求\",\"important\":\"X\"}]}");
    f.service.repairMultiContentNames(f.task, f.config, 1L, List.of("X", "Y"));

    ArgumentCaptor<ExamStandardNodeEntity> cap =
        ArgumentCaptor.forClass(ExamStandardNodeEntity.class);
    verify(f.nodeMapper).updateById(cap.capture());
    assertEquals(11L, cap.getValue().getId());
    assertEquals("设备日常保养要求", cap.getValue().getTitle());
    // 告警：说明已重命名且保持条数/编码不变
    String warnings = f.capturedWarnings();
    assertTrue(warnings.contains("已按「只考核一个主要内容」重命名"), warnings);
  }

  @Test
  void keepsOriginalAndWarnsWhenStillNonCompliant() {
    Fixture f = newFixture("{\"points\":[{\"title\":\"周期与要求\",\"important\":\"X\"}]}");
    f.service.repairMultiContentNames(f.task, f.config, 1L, List.of("X", "Y"));

    verify(f.nodeMapper, never()).updateById(any(ExamStandardNodeEntity.class));
    String warnings = f.capturedWarnings();
    assertTrue(warnings.contains("疑似并列了两个内容"), warnings);
    assertTrue(warnings.contains("设备日常保养周期与要求"), warnings);
  }

  // ---------- prompt 接线（护栏文案已进入生成与补点 prompt） ----------

  @Test
  void theoryPromptsCarryNameHardRules() {
    String main = StandardLlmSummarizeService.buildUserPrompt("基本要求/职业道德",
        List.of("（1）设备保养周期；（2）设备保养要求。"), false, List.of("X", "Y"), null);
    assertTrue(main.contains("只考核一个主要内容"), main);
    assertTrue(main.contains("不得使用疑问句"));
    assertTrue(main.contains("设备日常保养周期与要求"), "应含反例");
    assertTrue(main.contains("概念、性质、特点、分类、方法、规则、原理"), "应含句式指引");

    String topup = StandardLlmSummarizeService.buildTopupUserPrompt("工作要求/设备保养",
        List.of("1.1.1 能保养设备"), List.of("设备日常维护保养内容"), List.of("X", "Y"), 2);
    assertTrue(topup.contains("只考核一个主要内容"), topup);
    assertTrue(topup.contains("不得使用疑问句"));
  }

  /** 重命名 prompt 携带反例与"条数必须一致"约束（从 mock 的 LLM 入参断言） */
  @Test
  void renamePromptCarriesRulesAndCountConstraint() {
    Fixture f = newFixture("{\"points\":[{\"title\":\"设备日常保养要求\",\"important\":\"X\"}]}");
    f.service.repairMultiContentNames(f.task, f.config, 1L, List.of("X", "Y"));
    String prompt = f.lastUserPrompt;
    assertTrue(prompt.contains("只考核一个主要内容"), prompt);
    assertTrue(prompt.contains("条数与顺序必须与上面严格一致"), prompt);
    assertTrue(prompt.contains("设备日常保养周期与要求"), "应把违规原名作为反例列出");
    assertTrue(prompt.contains("公差与配合"), "应声明行业标准复合术语可保留");
  }

  // ---------- 夹具 ----------

  private static ExamStandardNodeEntity leaf(Long id, String title, String source,
      String reviewStatus) {
    ExamStandardNodeEntity n = new ExamStandardNodeEntity();
    n.setId(id);
    n.setParentId(101L);
    n.setTaskId(1L);
    n.setNodeLevel(3);
    n.setDimension("theory");
    n.setNodeKey("A-A-00" + id);
    n.setLevelCode(0);
    n.setTitle(title);
    n.setSource(source);
    n.setReviewStatus(reviewStatus);
    return n;
  }

  private static final class Fixture {
    StandardLlmSummarizeService service;
    ExamStandardTaskMapper taskMapper;
    ExamStandardNodeMapper nodeMapper;
    ExamStandardTaskEntity task;
    ExamFaqModelConfigEntity config;
    String warningsJson;
    String lastUserPrompt;

    String capturedWarnings() {
      return warningsJson == null ? "" : warningsJson;
    }
  }

  private Fixture newFixture(String llmJson) {
    Fixture f = new Fixture();
    f.taskMapper = mock(ExamStandardTaskMapper.class);
    f.nodeMapper = mock(ExamStandardNodeMapper.class);
    FaqLlmClient llm = mock(FaqLlmClient.class);

    f.task = new ExamStandardTaskEntity();
    f.task.setId(1L);
    f.task.setWorkTypeTitle("重冶备料工");
    f.task.setParseWarnings("");
    when(f.taskMapper.selectById(1L)).thenReturn(f.task);
    when(f.taskMapper.selectById(1L)).thenReturn(f.task);
    // appendTaskWarnings 会再查一次任务并 updateById 写回 parseWarnings
    when(f.taskMapper.updateById(any(ExamStandardTaskEntity.class))).thenAnswer(inv -> {
      ExamStandardTaskEntity u = inv.getArgument(0);
      f.warningsJson = u.getParseWarnings();
      return 1;
    });

    ExamStandardNodeEntity l2 = new ExamStandardNodeEntity();
    l2.setId(101L);
    l2.setNodeKey("A-A");
    l2.setTitle("设备保养");
    l2.setLevelCode(0);
    when(f.nodeMapper.selectById(101L)).thenReturn(l2);
    when(f.nodeMapper.selectList(any())).thenReturn(List.of(
        leaf(11L, "设备日常保养周期与要求", "llm", "pending"),
        leaf(12L, "设备日常维护保养内容", "llm", "pending")));

    when(llm.chatCompletion(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
        any(), any(), org.mockito.ArgumentMatchers.anyBoolean(),
        org.mockito.ArgumentMatchers.anyLong())).thenAnswer(inv -> {
          f.lastUserPrompt = inv.getArgument(4);
          return llmJson;
        });

    f.config = new ExamFaqModelConfigEntity();
    f.config.setBaseUrl("https://example.test/v1");
    f.config.setApiKey("k");
    f.config.setModelId("m");
    f.config.setMaxTokens(1024);
    f.service = new StandardLlmSummarizeService(f.taskMapper, f.nodeMapper, llm,
        mock(FaqModelConfigApplicationService.class), mock(DictMapper.class),
        mock(AiGenerationDraftMapper.class), mock(ThreadPoolTaskExecutor.class));
    return f;
  }
}
