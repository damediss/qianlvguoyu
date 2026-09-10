package com.guoyu.platform.exam.standard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.entity.ExamStandardTaskEntity;
import com.guoyu.platform.exam.standard.mapper.ExamStandardNodeMapper;
import com.guoyu.platform.exam.standard.mapper.ExamStandardTaskMapper;
import com.guoyu.platform.system.mapper.DictMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 同级跨小节重复考评点修复（方案 A）单测：目标筛选（编码靠前保留）+ 原位重生链路
 * （排除清单含全表既有点、有界重试后仍重复的保留原点）。
 */
class StandardLlmDupRepairTest {

  private ExamStandardNodeEntity leaf(long id, long parentId, String key, String title) {
    ExamStandardNodeEntity e = new ExamStandardNodeEntity();
    e.setId(id);
    e.setParentId(parentId);
    e.setNodeKey(key);
    e.setTitle(title);
    return e;
  }

  private StandardLlmSummarizeService newService(ExamStandardNodeMapper nodeMapper,
      FaqLlmClient llmClient, ExamStandardTaskMapper taskMapper) {
    return new StandardLlmSummarizeService(taskMapper, nodeMapper, llmClient,
        mock(FaqModelConfigApplicationService.class), mock(DictMapper.class),
        mock(AiGenerationDraftMapper.class), mock(ThreadPoolTaskExecutor.class));
  }

  private ExamStandardTaskEntity task() {
    ExamStandardTaskEntity t = new ExamStandardTaskEntity();
    t.setId(999L);
    return t;
  }

  private ExamFaqModelConfigEntity config() {
    ExamFaqModelConfigEntity c = new ExamFaqModelConfigEntity();
    c.setBaseUrl("http://localhost");
    c.setApiKey("k");
    c.setModelId("m");
    return c;
  }

  private ExamStandardNodeEntity section(long id, String key) {
    ExamStandardNodeEntity l2 = new ExamStandardNodeEntity();
    l2.setId(id);
    l2.setNodeKey(key);
    l2.setTaskId(999L);
    l2.setLevelCode(3);
    l2.setDirection("默认");
    l2.setRelatedKnowledge("1. 检修计划编制\n2. 检修组织实施");
    return l2;
  }

  // ---------- 目标筛选（纯函数） ----------

  @Test
  void selectTargetsKeepsEarliestAndFlagsLater() {
    List<ExamStandardNodeEntity> leaves = List.of(
        leaf(1, 100, "A-B-A-001", "检修计划编制依据"),
        leaf(2, 200, "B-D-B-001", "检修计划编制依据"),
        leaf(3, 200, "B-D-B-002", "检修计划实施要求"));
    List<ExamStandardNodeEntity> targets =
        StandardLlmSummarizeService.selectDuplicateTargets(leaves);
    assertEquals(1, targets.size());
    assertEquals(2L, targets.get(0).getId(), "保留编码靠前者，靠后的列修复目标");
  }

  @Test
  void selectTargetsSubstringBothDirections() {
    // 互为子串视为相近（与告警口径一致）：后者包含前者 → 后者列目标
    List<ExamStandardNodeEntity> leaves = List.of(
        leaf(1, 100, "A-B-A-001", "通风设施完好标准"),
        leaf(2, 200, "B-A-A-012", "局部通风设施完好标准"),
        leaf(3, 300, "B-B-A-001", "风筒吊挂标准"));
    List<ExamStandardNodeEntity> targets =
        StandardLlmSummarizeService.selectDuplicateTargets(leaves);
    assertEquals(1, targets.size());
    assertEquals(2L, targets.get(0).getId());
  }

  @Test
  void selectTargetsChainAndShortTitleSkipped() {
    // 链式：第三个与首现重复同样列目标；单字标题（<2 字）不参与重复判定
    List<ExamStandardNodeEntity> leaves = List.of(
        leaf(1, 100, "A-B-A-001", "风门构筑"),
        leaf(2, 200, "B-A-A-001", "风门构筑"),
        leaf(3, 300, "B-B-A-001", "风门构筑"),
        leaf(4, 100, "A-B-A-002", "全"));
    List<ExamStandardNodeEntity> targets =
        StandardLlmSummarizeService.selectDuplicateTargets(leaves);
    assertEquals(2, targets.size());
    assertEquals(2L, targets.get(0).getId());
    assertEquals(3L, targets.get(1).getId());
  }

  @Test
  void selectTargetsNoDuplicateReturnsEmpty() {
    List<ExamStandardNodeEntity> leaves = List.of(
        leaf(1, 100, "A-B-A-001", "矿井通风基础理论"),
        leaf(2, 200, "B-A-A-001", "局部通风机安装要求"));
    assertTrue(StandardLlmSummarizeService.selectDuplicateTargets(leaves).isEmpty());
  }

  // ---------- 原位重生链路（Mockito） ----------

  @Test
  void repairReplacesLaterDuplicateInPlaceWithScopeWideExclusion() {
    ExamStandardNodeMapper nodeMapper = mock(ExamStandardNodeMapper.class);
    FaqLlmClient llmClient = mock(FaqLlmClient.class);
    ExamStandardTaskMapper taskMapper = mock(ExamStandardTaskMapper.class);

    ExamStandardNodeEntity keep = leaf(1, 100, "A-B-A-001", "检修计划编制依据");
    ExamStandardNodeEntity dup = leaf(2, 200, "B-D-B-001", "检修计划编制依据");
    ExamStandardNodeEntity sibling = leaf(3, 200, "B-D-B-002", "检修计划实施要求");
    when(nodeMapper.selectById(200L)).thenReturn(section(200, "B-D-B"));
    when(llmClient.chatCompletion(any(), any(), any(), any(), any(), anyInt(), any(), any(),
        anyBoolean(), any()))
        .thenReturn("{\"points\":[{\"title\":\"检修质量验收标准\",\"important\":\"Y\"}]}");

    StandardLlmSummarizeService service = newService(nodeMapper, llmClient, taskMapper);
    int repaired = service.repairScopeDuplicates(task(), "3|默认", List.of(keep, dup, sibling),
        config(), 10159L, List.of("X", "Y", "Z"));

    assertEquals(1, repaired);
    // 原位更新：保留 id/nodeKey，只换标题与重要程度
    ArgumentCaptor<ExamStandardNodeEntity> updateCaptor =
        ArgumentCaptor.forClass(ExamStandardNodeEntity.class);
    verify(nodeMapper).updateById(updateCaptor.capture());
    assertEquals(2L, updateCaptor.getValue().getId());
    assertEquals("检修质量验收标准", updateCaptor.getValue().getTitle());
    assertEquals("Y", updateCaptor.getValue().getImportant());

    // prompt 排除清单含本 scope 全部既有点（含被替换点原标题与其它小节保留点）
    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    verify(llmClient).chatCompletion(any(), any(), any(), any(), promptCaptor.capture(),
        anyInt(), any(), any(), anyBoolean(), any());
    String prompt = promptCaptor.getValue();
    assertTrue(prompt.contains("检修计划编制依据"), "排除清单应含被替换点原标题");
    assertTrue(prompt.contains("检修计划实施要求"), "排除清单应含同小节既有点");
  }

  @Test
  void repairKeepsOriginalWhenRegenStillDuplicate() {
    // 两轮重生都只产出重复点 → 不落库更新，原点保留（告警通道在最终状态上自然标记）
    ExamStandardNodeMapper nodeMapper = mock(ExamStandardNodeMapper.class);
    FaqLlmClient llmClient = mock(FaqLlmClient.class);
    ExamStandardTaskMapper taskMapper = mock(ExamStandardTaskMapper.class);

    ExamStandardNodeEntity keep = leaf(1, 100, "A-B-A-001", "检修计划编制依据");
    ExamStandardNodeEntity dup = leaf(2, 200, "B-D-B-001", "检修计划编制依据");
    when(nodeMapper.selectById(200L)).thenReturn(section(200, "B-D-B"));
    when(llmClient.chatCompletion(any(), any(), any(), any(), any(), anyInt(), any(), any(),
        anyBoolean(), any()))
        .thenReturn("{\"points\":[{\"title\":\"检修计划编制依据要求\",\"important\":\"X\"}]}");

    StandardLlmSummarizeService service = newService(nodeMapper, llmClient, taskMapper);
    int repaired = service.repairScopeDuplicates(task(), "3|默认", List.of(keep, dup), config(),
        10159L, List.of("X", "Y", "Z"));

    assertEquals(0, repaired, "重生结果仍与既有点相近时不应替换");
    verify(nodeMapper, never())
        .updateById(org.mockito.ArgumentMatchers.<ExamStandardNodeEntity>any());
  }

  @Test
  void repairRoundTwoUsesLiveTitlesFromRoundOne() {
    // 第 1 轮产出 1 个新点修复第 1 个重复点；第 2 轮的排除清单必须含第 1 轮的新标题，
    // 且 LLM 若再次产出与第 1 轮新点重复的内容会被过滤
    ExamStandardNodeMapper nodeMapper = mock(ExamStandardNodeMapper.class);
    FaqLlmClient llmClient = mock(FaqLlmClient.class);
    ExamStandardTaskMapper taskMapper = mock(ExamStandardTaskMapper.class);

    ExamStandardNodeEntity keep = leaf(1, 100, "A-B-A-001", "检修计划编制依据");
    ExamStandardNodeEntity dup1 = leaf(2, 200, "B-D-B-001", "检修计划编制依据");
    ExamStandardNodeEntity dup2 = leaf(3, 200, "B-D-B-002", "检修计划编制依据细则");
    when(nodeMapper.selectById(200L)).thenReturn(section(200, "B-D-B"));
    // 第 1 轮只回 1 个有效点；第 2 轮回的点与第 1 轮新点互为子串 → 应被过滤，dup2 保留原点
    when(llmClient.chatCompletion(any(), any(), any(), any(), any(), anyInt(), any(), any(),
        anyBoolean(), any()))
        .thenReturn("{\"points\":[{\"title\":\"检修质量验收标准\",\"important\":\"X\"}]}")
        .thenReturn("{\"points\":[{\"title\":\"检修质量验收\",\"important\":\"X\"}]}");

    StandardLlmSummarizeService service = newService(nodeMapper, llmClient, taskMapper);
    int repaired = service.repairScopeDuplicates(task(), "3|默认", List.of(keep, dup1, dup2),
        config(), 10159L, List.of("X", "Y", "Z"));

    assertEquals(1, repaired, "仅第 1 个点被修复；第 2 轮产出与第 1 轮新点相近被过滤");
    ArgumentCaptor<ExamStandardNodeEntity> updateCaptor =
        ArgumentCaptor.forClass(ExamStandardNodeEntity.class);
    verify(nodeMapper).updateById(updateCaptor.capture());
    assertEquals(2L, updateCaptor.getValue().getId());
  }
}
