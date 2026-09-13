package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow;
import com.guoyu.platform.exam.knowledgefix.dto.PendingFixRow;
import com.guoyu.platform.exam.knowledgefix.entity.ExamDetailKnowledgeFixEntity;
import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixBatchEntity;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamDetailKnowledgeFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamExamDetailFixMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixBatchMapper;
import com.guoyu.platform.exam.knowledgefix.mapper.ExamKnowledgeFixLlmLogMapper;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 知识修正分类编排单测（KF-09~19）。LLM 全 mock 桩响应；自一致/升级/卫兵/补跑真实走线程池。
 * 状态语义模拟：updateClassifyResult 修改内存 status 表，selectPendingWithContent 按 status 过滤
 * （与生产"分类后不再 pending"语义同构）。
 */
class KnowledgeFixClassifyServiceTest {

  private static final Long TENANT = 10159L;
  private static final Long USER = 9L;
  private static final Long BATCH_ID = 500L;
  private static final Long BATCH_NO = 501L;

  private ExamKnowledgeFixBatchMapper batchMapper;
  private ExamDetailKnowledgeFixMapper fixMapper;
  private ExamExamDetailFixMapper queryMapper;
  private ExamFaqModelConfigMapper modelConfigMapper;
  private FaqLlmClient llmClient;
  private ExamKnowledgeFixLlmLogMapper llmLogMapper;
  private ThreadPoolTaskExecutor llmExecutor;
  private KnowledgeFixClassifyService service;

  private ExamFaqModelConfigEntity cfgFlash;
  private ExamFaqModelConfigEntity cfgPro;
  private ExamKnowledgeFixBatchEntity batch;
  private final Map<Long, String> statusByRowId = new HashMap<>();
  private final List<Integer> stage2CallCounts = new ArrayList<>();

  @BeforeEach
  void setUp() {
    batchMapper = mock(ExamKnowledgeFixBatchMapper.class);
    fixMapper = mock(ExamDetailKnowledgeFixMapper.class);
    queryMapper = mock(ExamExamDetailFixMapper.class);
    modelConfigMapper = mock(ExamFaqModelConfigMapper.class);
    llmClient = mock(FaqLlmClient.class);
    llmLogMapper = mock(ExamKnowledgeFixLlmLogMapper.class);
    statusByRowId.clear();
    stage2CallCounts.clear();

    ThreadPoolTaskExecutor orchestrator = new ThreadPoolTaskExecutor();
    orchestrator.setCorePoolSize(1);
    orchestrator.setMaxPoolSize(1);
    orchestrator.setQueueCapacity(10);
    orchestrator.initialize();

    llmExecutor = new ThreadPoolTaskExecutor();
    llmExecutor.setCorePoolSize(3);
    llmExecutor.setMaxPoolSize(3);
    llmExecutor.setQueueCapacity(10);
    llmExecutor.initialize();

    cfgFlash = cfg("theory-gen-glm", "glm-5.3-flash");
    cfgPro = cfg("knowledge-fix-pro", "glm-5.3");
    batch = batchEntity();

    service = new KnowledgeFixClassifyService(batchMapper, fixMapper, queryMapper,
        modelConfigMapper, llmClient, new ObjectMapper(), orchestrator, llmExecutor,
        llmLogMapper, 3, 0L);
  }

  @AfterEach
  void tearDown() {
    llmExecutor.shutdown();
  }

  // ───────── 造数 ─────────

  private ExamFaqModelConfigEntity cfg(String code, String modelId) {
    ExamFaqModelConfigEntity c = new ExamFaqModelConfigEntity();
    c.setId(1L);
    c.setTenantId(TENANT);
    c.setModelCode(code);
    c.setModelId(modelId);
    c.setBaseUrl("https://open.bigmodel.cn/api/paas/v4");
    c.setApiKey("test-key");
    c.setStatus("enabled");
    c.setThinking("enabled");
    c.setReasoningEffort("low");
    c.setMaxTokens(1024);
    return c;
  }

  private ExamKnowledgeFixBatchEntity batchEntity() {
    ExamKnowledgeFixBatchEntity b = new ExamKnowledgeFixBatchEntity();
    b.setId(BATCH_ID);
    b.setTenantId(TENANT);
    b.setBatchNo(BATCH_NO);
    b.setTotal(3);
    b.setStatus(ExamKnowledgeFixBatchEntity.STATUS_PENDING);
    b.setCreatedBy(USER);
    return b;
  }

  private KnowledgeNodeRow node(int id, int courseId, int parentId, String title, int level, String important) {
    KnowledgeNodeRow n = new KnowledgeNodeRow();
    n.setId(id);
    n.setCourseId(courseId);
    n.setParentId(parentId);
    n.setTitle(title);
    n.setLevel(level);
    n.setImportant(important);
    return n;
  }

  private PendingFixRow row(long rowId, int detailId, int oldKnowledgeId, int courseId, String stem) {
    PendingFixRow r = new PendingFixRow();
    r.setRowId(rowId);
    r.setDetailId(detailId);
    r.setOldKnowledgeId(oldKnowledgeId);
    r.setCourseId(courseId);
    r.setContent("<p>" + stem + "</p>");
    r.setAnswer("[{\"content\":\"A\",\"isCorrect\":true,\"score\":1}]");
    r.setResolve(null);
    statusByRowId.put(rowId, "pending");
    return r;
  }

  /** 课程 190 树：L2=201 蒸发系统（L3: 301 蒸发器跑碱处理 / 302 蒸发站真空调整）；L2=202 焙烧系统（L3: 303 焙烧温度控制）；老节点 4192 工匠精神培养 */
  private List<KnowledgeNodeRow> courseTree() {
    List<KnowledgeNodeRow> nodes = new ArrayList<>();
    nodes.add(node(200, 190, 0, "蒸发与焙烧单元", 1, "X"));
    nodes.add(node(201, 190, 200, "蒸发系统", 2, "X"));
    nodes.add(node(202, 190, 200, "焙烧系统", 2, "Y"));
    nodes.add(node(301, 190, 201, "蒸发器跑碱处理", 3, "X"));
    nodes.add(node(302, 190, 201, "蒸发站真空调整", 3, "Y"));
    nodes.add(node(303, 190, 202, "焙烧温度控制", 3, "Z"));
    nodes.add(node(4192, 190, 201, "工匠精神培养", 3, "X"));
    return nodes;
  }

  /** L2/L3 分层桩：L1 阶段选 201，L2 阶段按脚本返回 */
  private void stubLlm(MovieStyle script) {
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> LlmChatCompletionResult.from(null, "{\"l2_id\":201}"));
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(),
        anyLong(), anyInt()))
        .thenAnswer(inv -> {
          int idx = stage2CallCounts.size();
          stage2CallCounts.add(idx);
          return script.answer(idx, inv.getArgument(4));
        });
  }

  /** 脚本接口：第 n 次 L3 调用返回的内容 */
  private interface MovieStyle {
    LlmChatCompletionResult answer(int callIndex, String userPrompt);
  }

  private LlmChatCompletionResult result(String json) {
    return new LlmChatCompletionResult(json, 10, 5, 15, 0);
  }

  private void stubCommonHappyPath() {
    when(batchMapper.selectById(BATCH_ID)).thenAnswer(inv -> batch);
    when(batchMapper.updateById(ArgumentMatchers.<com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixBatchEntity>any())).thenReturn(1);
    when(queryMapper.selectAllNodes()).thenReturn(courseTree());
    when(queryMapper.selectMayRowCount()).thenReturn(3L);
    when(fixMapper.updateClassifyResult(anyLong(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
      statusByRowId.put(inv.getArgument(0), inv.getArgument(5));
      return 1;
    });
    when(batchMapper.incrementClassified(anyLong(), anyString())).thenReturn(1);
    when(batchMapper.incrementFailed(anyLong())).thenReturn(1);
  }

  // ───────── KF-09~19 ─────────

  /** KF-09：node_id 越界（非本课程树）→ 行保持 pending，3 轮后入 flag（自动补跑后仍失败） */
  @Test
  void kf09_outOfTreeNodeIdEndsFlagged() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> idx == 0
        ? result("{\"l2_id\":201}")
        : result("{\"node_id\":99999,\"confidence\":0.9,\"reason\":\"x\"}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals("flag", statusByRowId.get(1L));
    verify(llmLogMapper, atLeast(1)).insert(ArgumentMatchers.<com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity>any());
  }

  /** KF-10：模型选中当前节点（4192 工匠精神培养）→ keep，knowledge 不变 */
  @Test
  void kf10_modelPicksCurrentNodeMeansKeep() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> result("{\"node_id\":4192,\"confidence\":0.9,\"reason\":\"确属素养范畴\"}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "工匠精神的内涵"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_KEEP, statusByRowId.get(1L));
  }

  /** KF-11：三采样 3/3 一致 → apply，自一致率 1.0，批次 DONE */
  @Test
  void kf11_unanimousVoteApplies() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"蒸发器跑碱对应\"}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    verify(fixMapper).updateClassifyResult(anyLong(), ArgumentMatchers.eq(301), any(), any(), any(),
        ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_APPLY));
    assertEquals(0, BigDecimalCompare(batch.getSelfConsistencyRate(), 1.0));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-12：2/3 多数 → apply，置信取多数票均值 */
  @Test
  void kf12_majorityVoteApplies() {
    stubCommonHappyPath();
    AtomicInteger callIdx = new AtomicInteger();
    stubLlm((idx, prompt) -> {
      int i = callIdx.getAndIncrement() % 3;
      return switch (i) {
        case 0, 1 -> result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
        default -> result("{\"node_id\":302,\"confidence\":0.8,\"reason\":\"x\"}");
      };
    });
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    verify(fixMapper).updateClassifyResult(anyLong(), ArgumentMatchers.eq(301), any(), any(), any(),
        ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_APPLY));
  }

  /** KF-13：三方各不同 → 升级旗舰（glm-5.3）复判，结果生效且 basis=llm-pro */
  @Test
  void kf13_escalatesToProModel() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> {
      if (idx == 0) {
        return result("{\"node_id\":301,\"confidence\":0.6,\"reason\":\"a\"}");
      }
      if (idx == 1) {
        return result("{\"node_id\":302,\"confidence\":0.6,\"reason\":\"b\"}");
      }
      return result("{\"node_id\":303,\"confidence\":0.6,\"reason\":\"c\"}");
    });

    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 升级调用必须走 pro 模型（glm-5.3）
    verify(llmClient, atLeast(1)).chatCompletionDetailed(anyString(), anyString(),
        ArgumentMatchers.eq("glm-5.3"), anyString(), ArgumentMatchers.contains("候选鉴定点（L3）"),
        anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt());
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-14：确定性卫兵——选中节点与题干零重合且非素养节点 → 强制 flag */
  @Test
  void kf14_guardForcesFlagOnUnrelatedPick() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> result("{\"node_id\":303,\"confidence\":0.95,\"reason\":\"焙烧温度控制\"}"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因排查"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_FLAG, statusByRowId.get(1L));
    verify(fixMapper).updateClassifyResult(anyLong(), ArgumentMatchers.eq(303), any(), any(),
        ArgumentMatchers.contains("卫兵拦截"),
        ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_FLAG));
  }

  /** KF-15：模型输出零信任——非法 JSON → 行失败计数，批次不中断 */
  @Test
  void kf15_invalidJsonFailsRowNotBatch() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> result("这不是 JSON"));
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    verify(batchMapper, atLeast(1)).incrementFailed(BATCH_NO);
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-16：失败自动补跑——首轮失败的行第二轮重试成功 */
  @Test
  void kf16_failedRowRetriedNextRound() {
    stubCommonHappyPath();
    stubLlm((idx, prompt) -> {
      if (idx == 0) {
        throw new RuntimeException("transient timeout");
      }
      return result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
    });
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 轮 1 失败，轮 2 成功 → apply
    assertEquals(ExamDetailKnowledgeFixEntity.STATUS_APPLY, statusByRowId.get(1L));
    assertEquals(ExamKnowledgeFixBatchEntity.STATUS_DONE, batch.getStatus());
  }

  /** KF-17：熔断——连续失败达阈值停止派发（不浪费后续调用），未派发行入 flag */
  @Test
  void kf17_breakerStopsDispatchOnConsecutiveFailures() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 7; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    AtomicInteger llmCalls = new AtomicInteger();
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), contains("候选鉴定点"),
        anyString(), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          llmCalls.incrementAndGet();
          throw new RuntimeException("boom");
        });
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), contains("候选分类"),
        anyString(), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          llmCalls.incrementAndGet();
          throw new RuntimeException("boom");
        });

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // 连续失败 5 触发熔断：派发停止，行全部落 flag（宁可不改）
    assertTrue(llmCalls.get() <= 10, "熔断后调用数应受限，实际 " + llmCalls.get());
    verify(fixMapper, atLeast(1)).updateClassifyResult(anyLong(), any(), any(), any(),
        ArgumentMatchers.contains("自动补跑"), ArgumentMatchers.eq(ExamDetailKnowledgeFixEntity.STATUS_FLAG));
  }

  /** KF-18：多租户——分类线程内租户上下文正确 */
  @Test
  void kf18_tenantContextInWorkerThreads() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      rows.add(row(i, i, 4192, 190, "蒸发器跑碱的原因" + i));
    }
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    List<Long> seen = new CopyOnWriteArrayList<>();
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          seen.add(TenantContextHolder.getTenantId());
          return result("{\"l2_id\":201}");
        });
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          seen.add(TenantContextHolder.getTenantId());
          return result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
        });

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    assertEquals(12, seen.size()); // 3 行 ×（1 stage1 + 3 stage2）= 12 次 LLM 调用，每次均记录租户
    for (Long t : seen) {
      assertEquals(TENANT, t);
    }
  }

  /** KF-19：审计落库——每次 LLM 调用写一行审计（含 token 与耗时） */
  @Test
  void kf19_llmCallsAudited() {
    stubCommonHappyPath();
    List<PendingFixRow> rows = List.of(row(1, 1, 4192, 190, "蒸发器跑碱的原因"));
    when(fixMapper.selectPendingWithContent()).thenAnswer(inv -> rows.stream()
        .filter(r -> "pending".equals(statusByRowId.get(r.getRowId()))).toList());
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选分类（L2）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> result("{\"l2_id\":201}"));
    when(llmClient.chatCompletionDetailed(anyString(), anyString(), anyString(), anyString(),
        ArgumentMatchers.contains("候选鉴定点（L3）"), anyInt(), any(), any(), anyBoolean(), anyLong(), anyInt()))
        .thenAnswer(inv -> {
          System.out.println("[dbg] LLM STAGE2 tail=" + inv.<String>getArgument(4, String.class).substring(Math.max(0, inv.<String>getArgument(4, String.class).length() - 12)));
          return result("{\"node_id\":301,\"confidence\":0.9,\"reason\":\"x\"}");
        });

    service.processBatch(BATCH_ID, BATCH_NO, TENANT, USER, cfgFlash, cfgPro);

    // stage1 + 3×stage2 = 4 次调用 → 4 条审计
    ArgumentCaptor<com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity> cap = ArgumentCaptor.forClass(com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity.class);
    verify(llmLogMapper, times(4)).insert(cap.capture());
    for (com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixLlmLogEntity e2 : cap.getAllValues()) {
      System.out.println("[dbg] AUDIT stage=" + e2.getStage() + " model=" + e2.getModelId() + " tokens=" + e2.getTotalTokens());
    }
  }


  private static int BigDecimalCompare(java.math.BigDecimal actual, double expected) {
    return actual == null ? -1 : actual.compareTo(java.math.BigDecimal.valueOf(expected));
  }
}
