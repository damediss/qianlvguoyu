package com.guoyu.platform.exam.skillgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillBlueprintNodeMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenDraftMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** 批次提交单测：发起预检（S9/S10）、写库先提交后投递、拒绝路径、GLM 配置校验 */
class SkillGenBatchServiceTest {

  private static final Long TENANT = 1L;
  private static final Long USER = 9L;
  private static final Long BP_ID = 100L;
  private static final Long CFG_ID = 200L;

  private SkillBlueprintMapper blueprintMapper;
  private SkillBlueprintNodeMapper nodeMapper;
  private SkillGenBatchMapper batchMapper;
  private SkillGenDraftMapper draftMapper;
  private ExamFaqModelConfigMapper modelConfigMapper;
  private SkillGenAiService aiService;
  private SkillBlueprintApplicationService blueprintService;
  private TransactionTemplate transactionTemplate;
  private ThreadPoolTaskExecutor orchestrator;
  private SkillGenBatchService service;

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, SkillGenDraftEntity.class);
    TableInfoHelper.initTableInfo(assistant, SkillBlueprintNodeEntity.class);
    TableInfoHelper.initTableInfo(assistant, SkillGenBatchEntity.class);
  }

  @BeforeEach
  void setUp() {
    blueprintMapper = mock(SkillBlueprintMapper.class);
    nodeMapper = mock(SkillBlueprintNodeMapper.class);
    batchMapper = mock(SkillGenBatchMapper.class);
    draftMapper = mock(SkillGenDraftMapper.class);
    modelConfigMapper = mock(ExamFaqModelConfigMapper.class);
    aiService = mock(SkillGenAiService.class);
    blueprintService = mock(SkillBlueprintApplicationService.class);
    transactionTemplate = mock(TransactionTemplate.class);
    org.mockito.Mockito.doAnswer(inv -> {
      Consumer<TransactionStatus> consumer = inv.getArgument(0);
      consumer.accept(mock(TransactionStatus.class));
      return null;
    }).when(transactionTemplate).executeWithoutResult(any());
    orchestrator = mock(ThreadPoolTaskExecutor.class);
    ThreadPoolTaskExecutor blockExecutor = mock(ThreadPoolTaskExecutor.class);
    service = new SkillGenBatchService(blueprintMapper, nodeMapper, batchMapper, draftMapper,
        modelConfigMapper, aiService, new SkillQuestionValidator(), blueprintService,
        new ObjectMapper(), orchestrator, blockExecutor, transactionTemplate, 5, 10000L);
  }

  private SkillBlueprintEntity blueprint() {
    SkillBlueprintEntity bp = new SkillBlueprintEntity();
    bp.setId(BP_ID);
    bp.setTenantId(TENANT);
    bp.setWorkTypeId(22);
    bp.setLevel(2);
    return bp;
  }

  private ExamFaqModelConfigEntity modelConfig() {
    ExamFaqModelConfigEntity cfg = new ExamFaqModelConfigEntity();
    cfg.setId(CFG_ID);
    cfg.setModelId("glm-5.3-flash");
    cfg.setModelCode("skill-gen-glm");
    cfg.setBaseUrl("https://open.bigmodel.cn/api/paas/v4");
    cfg.setApiKey("sk-test");
    cfg.setThinking("enabled");
    cfg.setReasoningEffort("high");
    cfg.setMaxTokens(16384);
    cfg.setStatus("enabled");
    return cfg;
  }

  private List<SkillBlueprintNodeEntity> points(int count) {
    List<SkillBlueprintNodeEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      SkillBlueprintNodeEntity n = new SkillBlueprintNodeEntity();
      n.setId(10L + i);
      n.setTenantId(TENANT);
      n.setBlueprintId(BP_ID);
      n.setNodeLevel(3);
      n.setFullCode("A-A-" + String.format("%03d", i + 1));
      n.setName("考评点" + (i + 1));
      list.add(n);
    }
    return list;
  }

  /** 回归：pause→快速 resume 竞态——旧编排器在飞块（未落草稿）已登记的点不得再派发 */
  @Test
  void gen3_pendingNodesSkipsInFlightDispatchedNodes() {
    List<SkillBlueprintNodeEntity> points = points(3);
    java.util.Set<Long> done = new java.util.HashSet<>();          // DB 尚无草稿
    java.util.Set<Long> dispatched = new java.util.HashSet<>();
    dispatched.add(points.get(1).getId());                          // 旧编排器在飞
    List<SkillBlueprintNodeEntity> pending = SkillGenBatchService.pendingNodes(points, done, dispatched);
    assertEquals(2, pending.size(), "在飞登记点必须跳过，否则重复生成（实测 27/23 竞态）");
    assertTrue(pending.stream().noneMatch(n -> n.getId().equals(points.get(1).getId())));
    // 已有草稿点同样跳过（断点续跑）
    done.add(points.get(0).getId());
    assertEquals(1, SkillGenBatchService.pendingNodes(points, done, dispatched).size());
  }

  /** 回归：库内防重——该点已有草稿时 saveDrafts 不落库、不计数 */
  @Test
  void gen3_saveDraftsSkipsWhenNodeAlreadyHasDrafts() {
    SkillGenBatchEntity batch = new SkillGenBatchEntity();
    batch.setId(300L);
    batch.setTenantId(TENANT);
    batch.setBlueprintId(BP_ID);
    batch.setCreatedBy(USER);
    ExamFaqModelConfigEntity cfg = modelConfig();
    SkillBlueprintNodeEntity point = points(1).get(0);
    when(draftMapper.selectCount(any())).thenReturn(5L);   // 已有草稿
    List<com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem> questions = new ArrayList<>();
    boolean saved = service.saveDrafts(batch, cfg, point, null, questions, 100);
    assertEquals(false, saved, "已有草稿必须跳过落库");
    verify(draftMapper, never()).insert(any(SkillGenDraftEntity.class));
  }

  @Test
  void submit_precheckBlocksWhenPlannedPlusCumulativeBelowFloor() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(modelConfig());
    when(nodeMapper.selectList(any())).thenReturn(points(4)); // 4×5=20
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(5, 5)); // 20+5=25 < 30

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID));
    assertTrue(ex.getMessage().contains("低于本等级题库总量下限"));
    assertTrue(ex.getMessage().contains("还差"));
    verify(batchMapper, never()).insert(any(SkillGenBatchEntity.class));
  }

  @Test
  void submit_successInsertsBatchThenDispatches() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(modelConfig());
    when(nodeMapper.selectList(any())).thenReturn(points(10)); // 10×5=50 ≥ 30
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(0, 0));

    var vo = service.submit(TENANT, USER, BP_ID, CFG_ID);

    assertEquals(SkillGenBatchEntity.STATUS_PENDING, vo.status());
    assertEquals(10, vo.totalPoints());
    ArgumentCaptor<SkillGenBatchEntity> cap = ArgumentCaptor.forClass(SkillGenBatchEntity.class);
    verify(batchMapper).insert(cap.capture());
    assertEquals("{\"perPoint\":5}", cap.getValue().getQuotaConfig());
    assertTrue(cap.getValue().getPromptDigest() != null && !cap.getValue().getPromptDigest().isBlank());
    // 写库先提交（TransactionTemplate）后投递编排线程
    verify(transactionTemplate).executeWithoutResult(any());
    verify(orchestrator).submit(any(Runnable.class));
  }

  @Test
  void submit_rejectedExecutorMarksBatchFailed() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(modelConfig());
    when(nodeMapper.selectList(any())).thenReturn(points(10));
    when(blueprintService.cumulativeStat(TENANT, 22, 2))
        .thenReturn(new SkillBlueprintApplicationService.CumulativeStat(0, 0));
    org.mockito.Mockito.doThrow(new RejectedExecutionException("full"))
        .when(orchestrator).submit(any(Runnable.class));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID));
    assertTrue(ex.getMessage().contains("服务繁忙"));
    verify(batchMapper).insert(any(SkillGenBatchEntity.class));
    ArgumentCaptor<SkillGenBatchEntity> updated = ArgumentCaptor.forClass(SkillGenBatchEntity.class);
    verify(batchMapper).updateById(updated.capture());
    assertEquals(SkillGenBatchEntity.STATUS_FAILED, updated.getValue().getStatus());
    assertTrue(updated.getValue().getFailReason().contains("服务繁忙"));
  }

  @Test
  void submit_glmDisabledThinkingRejected() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    ExamFaqModelConfigEntity cfg = modelConfig();
    cfg.setThinking("disabled");
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(cfg);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID));
    assertTrue(ex.getMessage().contains("不可关闭思考"));
  }

  @Test
  void submit_rejectsApiKeyPlaceholder() {
    when(blueprintMapper.selectById(BP_ID)).thenReturn(blueprint());
    when(batchMapper.selectCount(any())).thenReturn(0L);
    ExamFaqModelConfigEntity cfg = modelConfig();
    cfg.setApiKey("PENDING_USER_INPUT");
    when(modelConfigMapper.selectById(CFG_ID)).thenReturn(cfg);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.submit(TENANT, USER, BP_ID, CFG_ID));
    assertTrue(ex.getMessage().contains("api_key 未配置"));
  }

  // ---------- T11：runBlock 回归（单题拆分后 usage 累计、传参口径、失败计数） ----------

  private static void invokeRunBlock(SkillGenBatchService service, SkillGenBatchEntity batch,
      ExamFaqModelConfigEntity cfg, SkillBlueprintNodeEntity point,
      java.util.Map<Long, SkillBlueprintNodeEntity> nodeMap,
      java.util.concurrent.atomic.AtomicInteger failures) throws Exception {
    java.lang.reflect.Method m = SkillGenBatchService.class.getDeclaredMethod("runBlock",
        SkillGenBatchEntity.class, ExamFaqModelConfigEntity.class, String.class,
        SkillBlueprintNodeEntity.class, java.util.Map.class,
        java.util.concurrent.atomic.AtomicInteger.class);
    m.setAccessible(true);
    m.invoke(service, batch, cfg, "sys", point, nodeMap, failures);
  }

  private static SkillQuestionItem validItem(String name) {
    SkillQuestionItem it = new SkillQuestionItem();
    it.setName(name);
    it.setDurationMinutes(20);
    it.setExamForm("实操");
    SkillQuestionItem.SitePrepare sp = new SkillQuestionItem.SitePrepare();
    SkillQuestionItem.Equipment eq = new SkillQuestionItem.Equipment();
    eq.setName("考核设备");
    eq.setUnit("台");
    eq.setQty("1");
    sp.setEquipment(java.util.List.of(eq));
    it.setSitePrepare(sp);
    it.setExamineePrepare("本人身份证×1、准考证×1、黑色签字笔×1");
    it.setRequirements("请完成" + name + "规定的考核操作");
    it.setNegatives("若考生发生下列情况之一，则应及时终止其考试，考生该试题成绩记为零分：违反安全操作规程");
    it.setScoring(java.util.List.of(
        scoringItem("要素一", "50", "50", deductDesc(10)),
        scoringItem("要素二", "30", "30", deductDesc(6)),
        scoringItem("要素三", "20", "20", deductDesc(4))));
    return it;
  }

  private static SkillQuestionItem.ScoringItem scoringItem(String item, String score,
      String maxDeduct, String desc) {
    SkillQuestionItem.ScoringItem s = new SkillQuestionItem.ScoringItem();
    s.setItem(item);
    s.setScore(score);
    s.setScoreDesc(desc);
    s.setMaxDeduct(maxDeduct);
    s.setStep("5");
    return s;
  }

  private static String deductDesc(int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      sb.append("考核点未达标扣5分；");
    }
    return sb.toString();
  }

  private SkillGenBatchEntity batch() {
    SkillGenBatchEntity batch = new SkillGenBatchEntity();
    batch.setId(300L);
    batch.setTenantId(TENANT);
    batch.setBlueprintId(BP_ID);
    batch.setCreatedBy(USER);
    return batch;
  }

  @Test
  void runBlock_singleSplitCountsCumulativeUsage() throws Exception {
    ExamFaqModelConfigEntity cfg = modelConfig();
    SkillBlueprintNodeEntity point = points(1).get(0);
    List<SkillQuestionItem> items = List.of(validItem("题一"), validItem("题二"),
        validItem("题三"), validItem("题四"), validItem("题五"));
    // attempts = 总调用次数：5 题无重试 → 5；此处给 6 表示第某题重试过一次
    when(aiService.generateForPointWithRepair(any(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), anyString(), anyString(), anyInt()))
        .thenReturn(new SkillGenAiService.SkillGenGeneration(items, "[...]", 6,
            100, 200, 300, 10));
    when(transactionTemplate.execute(any())).thenReturn(true);
    when(draftMapper.selectCount(any())).thenReturn(0L);

    java.util.concurrent.atomic.AtomicInteger failures = new java.util.concurrent.atomic.AtomicInteger();
    invokeRunBlock(service, batch(), cfg, point, java.util.Map.of(), failures);

    // 重试过也按 5 题计数，usage 用累计值
    verify(batchMapper).incrementDone(eq(300L), eq(5), eq(300L), eq(100L), eq(200L), eq(10L));
    assertEquals(0, failures.get());
    // runBlock 不再自行解析（解析/校验在 AI 服务内随单题重试完成）
    verify(aiService, never()).parseQuestions(any());
    // 传给 AI 服务的必须是「基础信息模板」，不得再是 5 题数组契约
    ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
    verify(aiService).generateForPointWithRepair(any(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), anyString(), userPrompt.capture(), anyInt());
    assertTrue(userPrompt.getValue().contains("考评点代码：A-A-001"));
    assertTrue(userPrompt.getValue().contains("考评点名称：考评点1"));
    assertTrue(userPrompt.getValue().contains("重要程度："), "基础信息模板应含重要程度");
    assertTrue(!userPrompt.getValue().contains("JSON"),
        "基础信息模板不得含输出契约（单题指令由 AI 服务按题序追加）");
  }

  @Test
  void runBlock_repairExhaustedCountsFailure() throws Exception {
    ExamFaqModelConfigEntity cfg = modelConfig();
    SkillBlueprintNodeEntity point = points(1).get(0);
    when(aiService.generateForPointWithRepair(any(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), anyString(), anyString(), anyInt()))
        .thenThrow(new BusinessException(com.guoyu.platform.common.exception.ErrorCode.BAD_GATEWAY,
            "第 3 题生成失败：模型输出不是单题对象（该题已调用 3 次）"));

    java.util.concurrent.atomic.AtomicInteger failures = new java.util.concurrent.atomic.AtomicInteger();
    invokeRunBlock(service, batch(), cfg, point, java.util.Map.of(), failures);

    // gen == null（整点抛错）：usage 按 0 计，但失败计数必须 +1
    verify(batchMapper).incrementFailed(eq(300L), eq(0L), eq(0L), eq(0L), eq(0L));
    verify(batchMapper, never()).incrementDone(any(), anyInt(), anyLong(), anyLong(), anyLong(), anyLong());
    assertEquals(1, failures.get());
  }

  @Test
  void runBlock_invalidOutputCountsFailureWithUsage() throws Exception {
    ExamFaqModelConfigEntity cfg = modelConfig();
    SkillBlueprintNodeEntity point = points(1).get(0);
    // 5 题但名称重复（第 5 题与第 1 题同名）：validator 拒绝，gen 已产出 usage 必须计入失败
    List<SkillQuestionItem> items = List.of(validItem("题一"), validItem("题二"),
        validItem("题三"), validItem("题四"), validItem("题一"));
    when(aiService.generateForPointWithRepair(any(), anyString(), anyString(), anyString(),
        anyString(), anyString(), anyInt(), anyString(), anyString(), anyInt()))
        .thenReturn(new SkillGenAiService.SkillGenGeneration(items, "[...]", 2,
            100, 200, 300, 10));
    when(transactionTemplate.execute(any())).thenReturn(true);
    when(draftMapper.selectCount(any())).thenReturn(0L);

    java.util.concurrent.atomic.AtomicInteger failures = new java.util.concurrent.atomic.AtomicInteger();
    invokeRunBlock(service, batch(), cfg, point, java.util.Map.of(), failures);

    verify(batchMapper, never()).incrementDone(any(), anyInt(), anyLong(), anyLong(), anyLong(), anyLong());
    verify(batchMapper).incrementFailed(eq(300L), eq(300L), eq(100L), eq(200L), eq(10L));
    assertEquals(1, failures.get());
  }
}
