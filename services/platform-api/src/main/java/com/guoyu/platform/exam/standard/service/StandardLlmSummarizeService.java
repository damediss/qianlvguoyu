package com.guoyu.platform.exam.standard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.exam.standard.dto.StandardSummarizeReq;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.entity.ExamStandardTaskEntity;
import com.guoyu.platform.exam.standard.mapper.ExamStandardNodeMapper;
import com.guoyu.platform.exam.standard.mapper.ExamStandardTaskMapper;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 标准细目表 LLM 概括（方案 §6）：按「L2 小节」逐块双通道调用（理论/技能），
 * 确定性护栏见 {@link #parsePointsResponse}；全部产出 pending 草稿 + ai_generation_draft 留痕。
 *
 * <p>线程边界：trigger 方法跑在 HTTP 线程（捕获 tenantId/userId），runSummarize 跑在线程池
 * Worker 线程（不调 SecurityContext，TenantContextHolder 手动 set/clear，坑#15）。
 */
@Service
public class StandardLlmSummarizeService {

  private static final Logger log = LoggerFactory.getLogger(StandardLlmSummarizeService.class);

  private static final String DICT_MODEL_CODE = "ai_standard_mubiao_model_code";
  private static final String DEFAULT_MODEL_CODE = "deepseek-review";
  private static final String DRAFT_TYPE = "standard_knowledge";
  /** 理论通道每张细目表（等级×工种）考评点总数目标：按小节权重分配，保底 1 点/小节，总量控制在 200~230 */
  static final int THEORY_TOTAL_TARGET = 200;
  /** 技能通道上限 = 原文条数 × 2 */
  static final int SKILL_MAX_FACTOR = 2;
  /** 解析失败最大重试次数（含首次共 3 次尝试） */
  static final int MAX_ATTEMPTS = 3;
  /** 小节级并行线程数（控制 LLM 并发与 DB 连接占用） */
  static final int SECTION_PARALLELISM = 3;
  /** 理论通道 max_tokens 下限（目标点数为 0 时兜底） */
  static final int THEORY_MIN_TOKENS = 1024;
  /** 理论通道 max_tokens 上限（按目标点数自适应后的封顶） */
  static final int THEORY_MAX_TOKENS = 4096;
  /** 技能通道 max_tokens 上限 */
  static final int SKILL_MAX_TOKENS = 2048;

  private final ExamStandardTaskMapper taskMapper;
  private final ExamStandardNodeMapper nodeMapper;
  private final FaqLlmClient llmClient;
  private final FaqModelConfigApplicationService modelConfigService;
  private final DictMapper dictMapper;
  private final AiGenerationDraftMapper draftMapper;
  private final ThreadPoolTaskExecutor executor;

  public StandardLlmSummarizeService(ExamStandardTaskMapper taskMapper,
      ExamStandardNodeMapper nodeMapper,
      FaqLlmClient llmClient,
      FaqModelConfigApplicationService modelConfigService,
      DictMapper dictMapper,
      AiGenerationDraftMapper draftMapper,
      @Qualifier("standardSummarizeExecutor") ThreadPoolTaskExecutor executor) {
    this.taskMapper = taskMapper;
    this.nodeMapper = nodeMapper;
    this.llmClient = llmClient;
    this.modelConfigService = modelConfigService;
    this.dictMapper = dictMapper;
    this.draftMapper = draftMapper;
    this.executor = executor;
  }

  // ============================================================
  // HTTP 线程入口
  // ============================================================

  /** 触发全任务 LLM 概括（异步）。同任务重复触发 → 409。 */
  public Long triggerSummarize(Long taskId, Long tenantId, Long userId) {
    return triggerSummarize(taskId, tenantId, userId, null);
  }

  /**
   * 触发 LLM 概括（异步）。
   *
   * @param req 弹窗选择：scopes=等级×工种组合（null=全部有效组合）；
   *            importants=理论通道重要程度档位（null=XYZ 全档）。
   *            同任务重复触发 → 409。
   */
  public Long triggerSummarize(Long taskId, Long tenantId, Long userId, StandardSummarizeReq req) {
    ExamStandardTaskEntity task = findTask(taskId, tenantId);
    if (StandardParseApplicationService.STATUS_LLM_RUNNING.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "该任务 LLM 概括正在进行中，请勿重复触发");
    }
    if (!List.of(StandardParseApplicationService.STATUS_PARSED,
        StandardParseApplicationService.STATUS_REVIEWING,
        StandardParseApplicationService.STATUS_CONFIRMED).contains(task.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "任务状态不允许触发概括：" + task.getStatus());
    }
    ExamFaqModelConfigEntity config = resolveModelConfig(tenantId);
    if (config == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "LLM 模型配置缺失（sys_dict[" + DICT_MODEL_CODE + "] 或默认 " + DEFAULT_MODEL_CODE
              + " 均未配置），任务保持 parsed 状态，可人工补叶后直接导出");
    }
    List<String> importants = normalizeImportants(req == null ? null : req.importants());
    Set<String> scopeKeys = resolveScopeKeys(task, req == null ? null : req.scopes());
    task.setStatus(StandardParseApplicationService.STATUS_LLM_RUNNING);
    task.setLlmModelId(config.getModelId());
    taskMapper.updateById(task);
    try {
      executor.execute(() -> runSummarize(taskId, tenantId, userId, scopeKeys, importants));
    } catch (Exception e) {
      task.setStatus(StandardParseApplicationService.STATUS_PARSED);
      taskMapper.updateById(task);
      log.warn("[细目表] LLM 线程池拒绝：taskId={} error={}", taskId, e.getMessage());
      throw new BusinessException("503", "服务繁忙，请稍后重试");
    }
    return taskId;
  }

  /**
   * 规范化重要程度档位：去重、大写、按 X→Y→Z 排序；null=XYZ 全档；空集或非法值 → 400。
   */
  public static List<String> normalizeImportants(List<String> raw) {
    if (raw == null) {
      return List.of("X", "Y", "Z");
    }
    Set<String> valid = new LinkedHashSet<>();
    for (String v : raw) {
      if (v == null) {
        continue;
      }
      String up = v.trim().toUpperCase();
      if (!List.of("X", "Y", "Z").contains(up)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "非法重要程度档位：" + v);
      }
      valid.add(up);
    }
    if (valid.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "重要程度至少选择一项（X/Y/Z）");
    }
    List<String> out = new ArrayList<>();
    for (String x : List.of("X", "Y", "Z")) {
      if (valid.contains(x)) {
        out.add(x);
      }
    }
    return out;
  }

  /**
   * 解析有效「等级×工种」组合集合（key=levelCode|direction）：
   * 有 directionLevels 矩阵时按其过滤无效组合；req.scopes 为 null 时返回全部有效组合。
   */
  private Set<String> resolveScopeKeys(ExamStandardTaskEntity task,
      List<StandardSummarizeReq.Scope> scopes) {
    Set<String> valid = new LinkedHashSet<>();
    java.util.Map<String, List<Integer>> matrix = readDirectionLevels(task.getDirectionLevelsJson());
    List<String> directions = readStringList(task.getDirectionsJson());
    List<java.util.Map<String, Object>> levels = readLevels(task.getLevelsJson());
    for (java.util.Map<String, Object> level : levels) {
      int levelCode = ((Number) level.get("levelCode")).intValue();
      for (String direction : directions) {
        List<Integer> allowed = matrix.get(direction);
        if (allowed != null && !allowed.contains(levelCode)) {
          continue;
        }
        valid.add(levelCode + "|" + direction);
      }
    }
    if (scopes == null) {
      return valid;
    }
    Set<String> selected = new LinkedHashSet<>();
    for (StandardSummarizeReq.Scope s : scopes) {
      if (s == null || s.levelCode() == null || s.direction() == null) {
        continue;
      }
      String key = s.levelCode() + "|" + s.direction();
      if (valid.contains(key)) {
        selected.add(key);
      } else {
        log.warn("[细目表] 忽略无效概括组合：taskId={} scope={}", task.getId(), key);
      }
    }
    if (selected.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未选择有效的工种×等级组合");
    }
    return selected;
  }

  // ---- task JSON 字段读取（与 StandardParseApplicationService 同规则） ----

  private static List<String> readStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return new ObjectMapper().readValue(json,
          new ObjectMapper().getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (Exception e) {
      return List.of();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<java.util.Map<String, Object>> readLevels(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return new ObjectMapper().readValue(json, List.class);
    } catch (Exception e) {
      return List.of();
    }
  }

  private static java.util.Map<String, List<Integer>> readDirectionLevels(String json) {
    if (json == null || json.isBlank()) {
      return java.util.Map.of();
    }
    try {
      return new ObjectMapper().readValue(json,
          new com.fasterxml.jackson.core.type.TypeReference<
              java.util.LinkedHashMap<String, List<Integer>>>() {});
    } catch (Exception e) {
      log.warn("[细目表] directionLevelsJson 解析失败: {}", e.getMessage());
      return java.util.Map.of();
    }
  }

  /** 单小节重新生成（同步，按 L2 节点 dimension 走对应通道） */
  @Transactional
  public List<ExamStandardNodeEntity> resummarizeSection(Long l2NodeId, String dimension,
      Long tenantId, Long userId) {
    ExamStandardNodeEntity l2 = findNode(l2NodeId, tenantId);
    if (l2.getNodeLevel() != 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持对二级小节（L2）重新生成");
    }
    String dim = "skill".equals(dimension) ? "skill" : "theory";
    if ("skill".equals(dim) && (l2.getSkillRequirement() == null || l2.getSkillRequirement().isBlank())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该小节无技能要求素材，无法走技能通道");
    }
    ExamStandardTaskEntity task = findTask(l2.getTaskId(), tenantId);
    ExamFaqModelConfigEntity config = resolveModelConfig(tenantId);
    if (config == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "LLM 模型配置缺失，无法重新生成");
    }
    // 清掉本小节该维度的既有 LLM 草稿叶（保留人工/已审改节点）
    nodeMapper.delete(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getParentId, l2NodeId)
        .eq(ExamStandardNodeEntity::getDimension, dim)
        .eq(ExamStandardNodeEntity::getSource, "llm")
        .eq(ExamStandardNodeEntity::getReviewStatus, "pending"));
    List<String> materials = "skill".equals(dim)
        ? splitMaterials(l2.getSkillRequirement()) : splitMaterials(l2.getRelatedKnowledge());
    // 理论通道：按本小节在所属「等级×工种」内的权重算出目标点数（与全量概括同一规则）
    Integer theoryTarget = null;
    if (!"skill".equals(dim)) {
      theoryTarget = computeSectionTarget(l2);
    }
    List<ExamStandardNodeEntity> created = summarizeOneSection(task, l2, dim, materials,
        config, tenantId, userId, List.of("X", "Y", "Z"), theoryTarget);
    log.info("[细目表] 单小节重新生成：nodeId={} dim={} 新叶={}", l2NodeId, dim, created.size());
    return created;
  }

  /** 单小节理论目标点数：加载所属 scope 的 L1+L2，跑同一套按权重分配后取本小节的值 */
  private Integer computeSectionTarget(ExamStandardNodeEntity l2) {
    List<ExamStandardNodeEntity> scopeNodes = nodeMapper.selectList(
        new LambdaQueryWrapper<ExamStandardNodeEntity>()
            .eq(ExamStandardNodeEntity::getTaskId, l2.getTaskId())
            .eq(ExamStandardNodeEntity::getLevelCode, l2.getLevelCode())
            .eq(ExamStandardNodeEntity::getDirection, l2.getDirection())
            .in(ExamStandardNodeEntity::getNodeLevel, 1, 2));
    List<ExamStandardNodeEntity> l1Nodes = scopeNodes.stream()
        .filter(n -> n.getNodeLevel() == 1).toList();
    List<ExamStandardNodeEntity> sections = scopeNodes.stream()
        .filter(n -> n.getNodeLevel() == 2).toList();
    return allocateTheoryTargets(l1Nodes, sections,
        s -> splitMaterials(s.getRelatedKnowledge()).size()).getOrDefault(l2.getId(), 1);
  }

  // ============================================================
  // Worker 线程：全任务概括
  // ============================================================

  private void runSummarize(Long taskId, Long tenantId, Long userId, Set<String> scopeKeys,
      List<String> importants) {
    TenantContextHolder.setTenantId(tenantId);
    ExecutorService sectionPool = null;
    try {
      ExamStandardTaskEntity task = taskMapper.selectById(taskId);
      if (task == null) {
        return;
      }
      ExamFaqModelConfigEntity config = resolveModelConfig(tenantId);
      if (config == null) {
        markTaskStatus(taskId, StandardParseApplicationService.STATUS_PARSED);
        return;
      }
      // 一次性加载全部 L2 小节（坑#18：批处理内不变数据加载复用），再按弹窗范围过滤
      List<ExamStandardNodeEntity> allSections = nodeMapper.selectList(
          new LambdaQueryWrapper<ExamStandardNodeEntity>()
              .eq(ExamStandardNodeEntity::getTaskId, taskId)
              .eq(ExamStandardNodeEntity::getNodeLevel, 2)
              .orderByAsc(ExamStandardNodeEntity::getLevelCode)
              .orderByAsc(ExamStandardNodeEntity::getDirection)
              .orderByAsc(ExamStandardNodeEntity::getNodeKey));
      List<ExamStandardNodeEntity> sections = new ArrayList<>();
      for (ExamStandardNodeEntity l2 : allSections) {
        if (scopeKeys.contains(l2.getLevelCode() + "|" + l2.getDirection())) {
          sections.add(l2);
        }
      }
      log.info("[细目表] LLM 概括范围：taskId={} 选中小节={}/{} 档位={}",
          taskId, sections.size(), allSections.size(), importants);

      // 按「等级×工种」分配每个小节的理论考评点目标数（拍板：全表 ≈200 点，按权重分配）
      List<ExamStandardNodeEntity> l1Nodes = nodeMapper.selectList(
          new LambdaQueryWrapper<ExamStandardNodeEntity>()
              .eq(ExamStandardNodeEntity::getTaskId, taskId)
              .eq(ExamStandardNodeEntity::getNodeLevel, 1));
      Map<Long, Integer> theoryTargets = new HashMap<>();
      Map<String, List<ExamStandardNodeEntity>> sectionsByScope = new LinkedHashMap<>();
      for (ExamStandardNodeEntity l2 : sections) {
        sectionsByScope.computeIfAbsent(l2.getLevelCode() + "|" + l2.getDirection(),
            k -> new ArrayList<>()).add(l2);
      }
      for (Map.Entry<String, List<ExamStandardNodeEntity>> e : sectionsByScope.entrySet()) {
        String[] parts = e.getKey().split("\\|", 2);
        int levelCode = Integer.parseInt(parts[0]);
        String direction = parts[1];
        List<ExamStandardNodeEntity> scopeL1 = l1Nodes.stream()
            .filter(n -> n.getLevelCode() != null && n.getLevelCode() == levelCode
                && direction.equals(n.getDirection()))
            .toList();
        Map<Long, Integer> targets = allocateTheoryTargets(scopeL1, e.getValue(),
            s -> splitMaterials(s.getRelatedKnowledge()).size());
        theoryTargets.putAll(targets);
        int sum = targets.values().stream().mapToInt(Integer::intValue).sum();
        log.info("[细目表] 理论点数分配：taskId={} scope={} 小节数={} 目标总数={}",
            taskId, e.getKey(), e.getValue().size(), sum);
      }

      // ---- 小节级并行：3 线程（A2），TenantContextHolder 手动传递（坑#15） ----
      sectionPool = Executors.newFixedThreadPool(SECTION_PARALLELISM, r -> {
        Thread t = new Thread(r, "standard-section-worker");
        t.setDaemon(true);
        return t;
      });
      AtomicInteger success = new AtomicInteger();
      AtomicInteger failed = new AtomicInteger();

      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (ExamStandardNodeEntity l2 : sections) {
        final ExamStandardNodeEntity section = l2;
        futures.add(CompletableFuture.runAsync(() -> {
          TenantContextHolder.setTenantId(tenantId);
          try {
            try {
              summarizeOneSection(task, section, "theory",
                  splitMaterials(section.getRelatedKnowledge()), config, tenantId, userId,
                  importants, theoryTargets.getOrDefault(section.getId(), 1));
              success.incrementAndGet();
            } catch (Exception e) {
              failed.incrementAndGet();
              log.warn("[细目表] 理论通道小节失败：taskId={} node={} error={}", taskId,
                  section.getNodeKey(), e.getMessage());
            }
            if (section.getSkillRequirement() != null && !section.getSkillRequirement().isBlank()) {
              try {
                summarizeOneSection(task, section, "skill",
                    splitMaterials(section.getSkillRequirement()), config, tenantId, userId,
                    importants, null);
                success.incrementAndGet();
              } catch (Exception e) {
                failed.incrementAndGet();
                log.warn("[细目表] 技能通道小节失败：taskId={} node={} error={}", taskId,
                    section.getNodeKey(), e.getMessage());
              }
            }
          } finally {
            TenantContextHolder.clear();
          }
        }, sectionPool));
      }
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      ExamStandardTaskEntity done = new ExamStandardTaskEntity();
      done.setId(taskId);
      done.setStatus(StandardParseApplicationService.STATUS_REVIEWING);
      done.setLlmFinishedAt(LocalDateTime.now());
      taskMapper.updateById(done);
      log.info("[细目表] LLM 概括完成：taskId={} 小节成功={} 失败={}", taskId, success.get(), failed.get());
    } catch (Exception e) {
      log.error("[细目表] LLM 概括异常：taskId={} error={}", taskId, e.getMessage(), e);
      markTaskStatus(taskId, StandardParseApplicationService.STATUS_PARSED);
    } finally {
      if (sectionPool != null) {
        sectionPool.shutdown();
      }
      TenantContextHolder.clear();
    }
  }

  private void markTaskStatus(Long taskId, String status) {
    ExamStandardTaskEntity update = new ExamStandardTaskEntity();
    update.setId(taskId);
    update.setStatus(status);
    taskMapper.updateById(update);
  }

  /**
   * 单小节单通道概括：LLM 调用 + 护栏 + 落叶节点 + 留痕。
   * LLM 彻底失败时按原文素材逐条兜底（source=extract，标记待人工——§6.2）。
   *
   * <p>importants 仅作用于理论通道：prompt 限定可选档位；理论通道数量由 theoryTarget 决定
   * （按小节权重从全表 200 点分配，优先级高于材料条数），越档产出归入最低允许档。
   * 技能通道保持「每条原文 1 点」一一对应。
   *
   * <p>生成前先清掉本小节该维度的 pending LLM 草稿叶（保留人工/已审改节点），保证重跑不重复。
   */
  private List<ExamStandardNodeEntity> summarizeOneSection(ExamStandardTaskEntity task,
      ExamStandardNodeEntity l2, String dimension, List<String> materials,
      ExamFaqModelConfigEntity config, Long tenantId, Long userId, List<String> importants,
      Integer theoryTarget) {
    boolean skill = "skill".equals(dimension);
    // 先清再生成：删除本小节该维度的 pending LLM 草稿叶
    nodeMapper.delete(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getParentId, l2.getId())
        .eq(ExamStandardNodeEntity::getDimension, dimension)
        .eq(ExamStandardNodeEntity::getSource, "llm")
        .eq(ExamStandardNodeEntity::getReviewStatus, "pending"));
    String sectionPath = sectionPath(task, l2);
    // C2：统一 system 前缀（不分理论/技能），差异下沉到 user prompt
    String systemPrompt = buildSystemPrompt(task, l2);
    String userPrompt = buildUserPrompt(sectionPath, materials, skill, importants, theoryTarget);
    // B2：max_tokens 按通道自适应（理论按目标点数，技能按素材条数）
    int maxTokens = skill ? computeMaxTokens(materials, true)
        : computeTheoryTokens(theoryTarget != null ? theoryTarget : 1);

    StandardLlmPoint.ParseResult result = null;
    String lastError = null;
    String lastContent = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      String prompt = attempt == 1 ? userPrompt
          : userPrompt + "\n\n上次输出解析失败：" + lastError + "。请严格以 JSON 输出 {\"points\":[...]}。";
      try {
        String content = llmClient.chatCompletion(config.getBaseUrl(), config.getApiKey(),
            config.getModelId(), systemPrompt, prompt, maxTokens,
            config.getThinking(), config.getReasoningEffort(), true, tenantId);
        lastContent = content;
        int maxCount = skill ? Math.max(1, materials.size() * SKILL_MAX_FACTOR)
            : Math.max(1, theoryTarget != null ? theoryTarget : 1);
        result = parsePointsResponse(content, maxCount, skill);
        if (!result.getPoints().isEmpty()) {
          break;
        }
        lastError = "points 为空";
        result = null;
      } catch (Exception e) {
        lastError = e.getMessage();
        log.warn("[细目表] LLM 第 {} 次尝试失败：taskId={} node={} dim={} error={}",
            attempt, task.getId(), l2.getNodeKey(), dimension, lastError);
      }
    }
    writeDraft(l2.getId(), systemPrompt + "\n" + userPrompt, lastContent != null ? lastContent : "",
        config.getModelId(), tenantId, dimension);

    List<ExamStandardNodeEntity> created = new ArrayList<>();
    if (result == null) {
      // 兜底：原文逐条为叶（extract + pending 待人工）
      List<String> fallback = materials.isEmpty() ? List.of() : materials;
      for (String material : fallback) {
        created.add(insertLeaf(l2, dimension, truncate(material, 200), "X", "extract",
            created.size(), tenantId));
      }
      log.warn("[细目表] 小节 LLM 失败，原文兜底 {} 条：node={} dim={}", created.size(), l2.getNodeKey(),
          dimension);
      return created;
    }
    // 理论通道：越档产出归入最低允许档（不丢弃考评点，防空结果）
    List<StandardLlmPoint> points = skill ? result.getPoints()
        : clampImportants(result.getPoints(), importants);
    for (StandardLlmPoint point : points) {
      created.add(insertLeaf(l2, dimension, point.title(), point.important(), "llm",
          created.size(), tenantId));
    }
    return created;
  }

  /**
   * 理论通道档位钳制：LLM 返回未选档位时，归入允许集合中的最低档（X&lt;Y&lt;Z，取列表末位）。
   * importants 必须已按 X→Y→Z 排序（见 {@link #normalizeImportants}）。
   */
  public static List<StandardLlmPoint> clampImportants(List<StandardLlmPoint> points,
      List<String> importants) {
    String lowest = importants.get(importants.size() - 1);
    List<StandardLlmPoint> out = new ArrayList<>(points.size());
    for (StandardLlmPoint p : points) {
      if (importants.contains(p.important())) {
        out.add(p);
      } else {
        log.warn("[细目表] 考评点档位越界：{} → 归入 {}（{}）", p.important(), lowest, p.title());
        out.add(new StandardLlmPoint(p.title(), lowest));
      }
    }
    return out;
  }

  private ExamStandardNodeEntity insertLeaf(ExamStandardNodeEntity l2, String dimension,
      String title, String important, String source, int index, Long tenantId) {
    ExamStandardNodeEntity leaf = new ExamStandardNodeEntity();
    leaf.setTenantId(tenantId);
    leaf.setTaskId(l2.getTaskId());
    leaf.setParentId(l2.getId());
    leaf.setNodeKey(NodeKeyGenerator.leafKey(l2.getNodeKey(), nextLeafIndex(l2.getId(), dimension, index)));
    leaf.setNodeLevel(3);
    leaf.setSortNumber(index + 1);
    leaf.setDimension(dimension);
    leaf.setTitle(title);
    leaf.setImportant(important);
    leaf.setSource(source);
    leaf.setReviewStatus("pending");
    leaf.setDirection(l2.getDirection());
    leaf.setLevelCode(l2.getLevelCode());
    leaf.setNeedLlm(0);
    nodeMapper.insert(leaf);
    return leaf;
  }

  private int nextLeafIndex(Long parentId, String dimension, int fallback) {
    Long count = nodeMapper.selectCount(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getParentId, parentId)
        .eq(ExamStandardNodeEntity::getDimension, dimension));
    return count == null ? fallback : count.intValue();
  }

  // ============================================================
  // 确定性护栏（静态可单测——B5/B6）
  // ============================================================

  /**
   * 解析 LLM JSON 输出并套用护栏：
   * 非法 JSON 抛异常（由调用方重试）；important 非法值默认 X + 警告；超 maxCount 截断 + 警告；
   * 技能通道标题以"能"字开头记警告（B6）。
   */
  public static StandardLlmPoint.ParseResult parsePointsResponse(String content, int maxCount,
      boolean skillChannel) {
    StandardLlmPoint.ParseResult result = new StandardLlmPoint.ParseResult();
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("LLM 返回内容为空");
    }
    String json = content.trim();
    // 剥离 markdown 围栏
    if (json.startsWith("```")) {
      json = json.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
    }
    JsonNode root;
    try {
      root = new ObjectMapper().readTree(json);
    } catch (Exception e) {
      throw new IllegalArgumentException("LLM 输出非合法 JSON：" + e.getMessage());
    }
    JsonNode points = root.get("points");
    if (points == null || !points.isArray()) {
      throw new IllegalArgumentException("LLM 输出缺少 points 数组");
    }
    for (JsonNode node : points) {
      String title = node.path("title").asText("").trim();
      if (title.isEmpty()) {
        continue;
      }
      if (title.length() > 60) {
        result.getWarnings().add("WARN: 考评点超长截断：" + title.substring(0, 30) + "…");
        title = title.substring(0, 60);
      }
      String important = node.path("important").asText("X").trim().toUpperCase();
      if (!List.of("X", "Y", "Z").contains(important)) {
        result.getWarnings().add("WARN: 非法 important 值默认 X：" + important);
        important = "X";
      }
      if (skillChannel && title.startsWith("能")) {
        result.getWarnings().add("WARN: 技能考评点标题不应以\"能\"开头：" + title);
      }
      result.getPoints().add(new StandardLlmPoint(title, important));
    }
    if (result.getPoints().size() > maxCount) {
      result.getWarnings().add("WARN: 单小节考评点超上限截断：" + result.getPoints().size() + " > " + maxCount);
      return truncate(result, maxCount);
    }
    return result;
  }

  private static StandardLlmPoint.ParseResult truncate(StandardLlmPoint.ParseResult src,
      int maxCount) {
    StandardLlmPoint.ParseResult out = new StandardLlmPoint.ParseResult();
    for (int i = 0; i < maxCount; i++) {
      out.getPoints().add(src.getPoints().get(i));
    }
    out.getWarnings().addAll(src.getWarnings());
    return out;
  }

  // ============================================================
  // prompt 构造（C2：统一 system 前缀，差异在 user prompt 尾部）
  // ============================================================

  /**
   * 统一 system prompt：不分理论/技能，保证同一「职业+等级」下所有小节共享同一前缀，
   * 最大化 DeepSeek 上下文硬盘缓存命中率。
   */
  private String buildSystemPrompt(ExamStandardTaskEntity task, ExamStandardNodeEntity l2) {
    return "你是职业技能鉴定命题专家，正在为「" + task.getWorkTypeTitle() + " "
        + levelName(l2.getLevelCode()) + "」编写考评点。";
  }

  /**
   * B2：按通道和素材条数自适应 max_tokens。
   * 技能：素材条数 × 40 token + 256 开销，封顶 2048。
   */
  public static int computeMaxTokens(List<String> materials, boolean skill) {
    if (skill) {
      int n = Math.max(1, materials.size());
      return Math.min(SKILL_MAX_TOKENS, n * 40 + 256);
    }
    // 理论通道走 computeTheoryTokens（按目标点数）
    return THEORY_MIN_TOKENS;
  }

  /** 理论通道 max_tokens：目标点数 × ~60 token + 256 开销，下限 1024、封顶 4096 */
  public static int computeTheoryTokens(int targetPoints) {
    int tokens = Math.max(1, targetPoints) * 60 + 256;
    return Math.min(THEORY_MAX_TOKENS, Math.max(THEORY_MIN_TOKENS, tokens));
  }

  private static String buildUserPrompt(String sectionPath, List<String> materials, boolean skill,
      List<String> importants, Integer theoryTarget) {
    StringBuilder sb = new StringBuilder();
    // C2：小节路径+素材放最前（变量部分），要求/格式放最后（稳定部分）
    sb.append("小节路径：").append(sectionPath).append('\n');
    sb.append(skill ? "技能要求原文：\n" : "原文素材：\n");
    if (materials.isEmpty()) {
      sb.append("（本小节无列举项，请依据小节主题与职业标准常识概括）\n");
    } else {
      for (int i = 0; i < materials.size(); i++) {
        sb.append("（").append(i + 1).append("）").append(materials.get(i)).append('\n');
      }
    }
    // C2：理论/技能差异全部在 user prompt 尾部，system 前缀保持一致
    if (skill) {
      sb.append("要求（操作技能考评点）：\n"
          + "1. 每条原文概括 1 个操作技能考评点，动宾/行为短语 ≤25 字，保留关键对象与动作，"
          + "去掉\"能\"字能力句式（如「检查风门、风筒的损坏状况」）；\n"
          + "2. 每个考评点给重要程度：X=核心操作，Y=重要，Z=辅助/了解；\n"
          + "3. 以 JSON 输出（必须含 \"points\" 数组），格式："
          + "{\"points\":[{\"title\":\"…\",\"important\":\"X\"}]}");
    } else {
      int target = theoryTarget != null ? theoryTarget : 6;
      sb.append("要求（理论知识考评点）：\n");
      if (materials.isEmpty()) {
        sb.append("1. 依据小节主题概括 ").append(target)
            .append(" 个考评点，每个为 ≤20 字的名词性短语，不得照抄长句；\n");
      } else {
        // 数量按小节考评权重分配（优先级高于逐条 1~2 个的经验规则）
        sb.append("1. 概括 ").append(target)
            .append(" 个考评点（该数量按本小节考评权重占全表比例分配，必须严格遵守）；")
            .append("逐条对应上述知识小点，内容多的小点可拆为 2 个、内容少的小点可合并，")
            .append("每个为 ≤20 字的名词性短语，不得照抄长句；\n");
      }
      sb.append("2. ").append(theoryImportantRule(importants)).append("；\n")
          .append("3. 以 JSON 输出（必须含 \"points\" 数组），格式：")
          .append("{\"points\":[{\"title\":\"…\",\"important\":\"X\"}]}");
    }
    return sb.toString();
  }

  /**
   * 理论通道每小节目标点数分配（拍板：总数 ≈ 200 点/张细目表，按权重分配，优先级高于材料条数规则）。
   *
   * <p>权重来源优先级：小节手工权重（绝对值，直接 ×200）&gt; L1 权重 × 小节材料条数占比
   * &gt; L1 无权重时按材料条数在全表剩余预算内兜底。最大余数法取整使合计严格等于 200；
   * 每小节保底 1 点（保底造成的总量略超 200 允许，仍 ≤230）。
   *
   * @param l1Nodes       该「等级×工种」范围内的 L1 节点（取 weight，可空元素忽略）
   * @param sections      该范围内的 L2 小节
   * @param materialCount 小节材料条数函数（空小节应按 1 条保底，由调用方保证 ≥0）
   * @return 小节 id → 目标点数（≥1）
   */
  public static Map<Long, Integer> allocateTheoryTargets(List<ExamStandardNodeEntity> l1Nodes,
      List<ExamStandardNodeEntity> sections, ToIntFunction<ExamStandardNodeEntity> materialCount) {
    BigDecimal total = BigDecimal.valueOf(THEORY_TOTAL_TARGET);
    Map<Long, BigDecimal> l1Weight = new HashMap<>();
    for (ExamStandardNodeEntity l1 : l1Nodes) {
      if (l1.getId() != null && l1.getWeight() != null) {
        l1Weight.put(l1.getId(), l1.getWeight());
      }
    }
    // 按 L1 分组小节，保持树内顺序
    Map<Long, List<ExamStandardNodeEntity>> byL1 = new LinkedHashMap<>();
    List<ExamStandardNodeEntity> fallback = new ArrayList<>();
    for (ExamStandardNodeEntity l2 : sections) {
      byL1.computeIfAbsent(l2.getParentId(), k -> new ArrayList<>()).add(l2);
    }

    // 小数目标：手工权重的小节直接 200×w；其余先在所属 L1 预算内按材料占比分，L1 无权重进兜底池
    Map<Long, BigDecimal> decimal = new LinkedHashMap<>();
    BigDecimal consumed = BigDecimal.ZERO;
    List<ExamStandardNodeEntity> pool = new ArrayList<>();
    for (Map.Entry<Long, List<ExamStandardNodeEntity>> e : byL1.entrySet()) {
      BigDecimal l1Budget = l1Weight.containsKey(e.getKey())
          ? total.multiply(l1Weight.get(e.getKey())) : null;
      BigDecimal manualSum = BigDecimal.ZERO;
      List<ExamStandardNodeEntity> unweighted = new ArrayList<>();
      for (ExamStandardNodeEntity l2 : e.getValue()) {
        if (l2.getWeight() != null) {
          BigDecimal pts = total.multiply(l2.getWeight());
          decimal.put(l2.getId(), pts);
          manualSum = manualSum.add(pts);
        } else {
          unweighted.add(l2);
        }
      }
      if (l1Budget == null) {
        // L1 无权重：手工小节照常消费，其余进全表兜底池
        consumed = consumed.add(manualSum);
        pool.addAll(unweighted);
        continue;
      }
      // L1 实际消费 = max(L1 预算, 手工合计)：手工超预算时记警告，超出部分挤占兜底池
      BigDecimal l1Consumed = manualSum.compareTo(l1Budget) > 0 ? manualSum : l1Budget;
      if (manualSum.compareTo(l1Budget) > 0) {
        log.warn("[细目表] L1 小节手工权重合计 {} 点超出 L1 预算 {} 点，超出部分挤占全表兜底预算",
            manualSum, l1Budget);
      }
      consumed = consumed.add(l1Consumed);
      if (unweighted.isEmpty()) {
        continue;
      }
      BigDecimal poolBudget = l1Budget.subtract(manualSum).max(BigDecimal.ZERO);
      distributeByMaterials(unweighted, poolBudget, materialCount, decimal);
    }

    // 兜底池：剩余预算按材料条数占比分配
    BigDecimal fallbackBudget = total.subtract(consumed).max(BigDecimal.ZERO);
    if (consumed.compareTo(total) > 0) {
      log.warn("[细目表] 权重消费合计 {} 点超出总目标 {}，兜底小节将只能拿保底 1 点", consumed, total);
    }
    distributeByMaterials(pool, fallbackBudget, materialCount, decimal);

    // 最大余数法取整：合计严格等于 200（有小数预算时）
    Map<Long, Integer> targets = largestRemainder(decimal, THEORY_TOTAL_TARGET);
    // 每小节保底 1 点（拍板：总量允许略超 200、不超 230）
    for (ExamStandardNodeEntity l2 : sections) {
      targets.merge(l2.getId(), 1, Math::max);
    }
    return targets;
  }

  /** 按材料条数占比把 budget 摊到 sections 上，写入 decimal（材料 0 条按 1 条保底） */
  private static void distributeByMaterials(List<ExamStandardNodeEntity> sections, BigDecimal budget,
      ToIntFunction<ExamStandardNodeEntity> materialCount, Map<Long, BigDecimal> decimal) {
    if (sections.isEmpty()) {
      return;
    }
    int totalMat = sections.stream().mapToInt(s -> Math.max(1, materialCount.applyAsInt(s))).sum();
    for (ExamStandardNodeEntity s : sections) {
      BigDecimal share = budget.multiply(BigDecimal.valueOf(Math.max(1, materialCount.applyAsInt(s))))
          .divide(BigDecimal.valueOf(totalMat), 6, RoundingMode.HALF_UP);
      decimal.merge(s.getId(), share, BigDecimal::add);
    }
  }

  /** 最大余数法：把小数目标摊成整数，合计 = 小数总和四舍五入（权重完整时即 200） */
  private static Map<Long, Integer> largestRemainder(Map<Long, BigDecimal> decimal, int total) {
    Map<Long, Integer> result = new LinkedHashMap<>();
    if (decimal.isEmpty()) {
      return result;
    }
    // 小数总和（权重超支/缺失时可能不足 total，按实际和取整，不硬凑）
    int target = decimal.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(0, RoundingMode.HALF_UP).intValue();
    record Share(Long id, int base, BigDecimal rem, int idx) {}
    List<Share> shares = new ArrayList<>();
    int allocated = 0;
    int idx = 0;
    for (Map.Entry<Long, BigDecimal> e : decimal.entrySet()) {
      int base = e.getValue().intValue(); // 截断取整
      BigDecimal rem = e.getValue().remainder(BigDecimal.ONE);
      shares.add(new Share(e.getKey(), base, rem, idx++));
      allocated += base;
      result.put(e.getKey(), base);
    }
    // 余数从大到小补足（每份额至多 +1）
    shares.sort(Comparator.comparing(Share::rem).reversed().thenComparingInt(Share::idx));
    int leftover = target - allocated;
    for (int i = 0; leftover > 0 && i < shares.size(); i++, leftover--) {
      Share s = shares.get(i);
      result.merge(s.id(), 1, Integer::sum);
    }
    return result;
  }

  /** 理论通道重要程度规则文案：全档=原三档说明；子集=仅允许所选档位 */
  public static String theoryImportantRule(List<String> importants) {
    java.util.Map<String, String> desc = java.util.Map.of(
        "X", "X=核心必考", "Y", "Y=重要（法律法规、安全规程类）", "Z", "Z=一般了解");
    if (importants.size() == 3) {
      return "每个考评点给重要程度：" + desc.get("X") + "，" + desc.get("Y") + "，" + desc.get("Z");
    }
    StringBuilder sb = new StringBuilder("每个考评点给重要程度，仅允许取值 ");
    sb.append(String.join("、", importants)).append("：");
    for (int i = 0; i < importants.size(); i++) {
      if (i > 0) {
        sb.append("，");
      }
      sb.append(desc.get(importants.get(i)));
    }
    return sb.toString();
  }

  private String sectionPath(ExamStandardTaskEntity task, ExamStandardNodeEntity l2) {
    String root = l2.getNodeKey().startsWith("A") ? "基本要求" : "工作要求";
    return root + "/" + l2.getTitle();
  }

  private String levelName(int levelCode) {
    return switch (levelCode) {
      case 0 -> "五级/初级工";
      case 1 -> "四级/中级工";
      case 2 -> "三级/高级工";
      case 3 -> "二级/技师";
      case 4 -> "一级/高级技师";
      default -> "";
    };
  }

  /** 相关知识/技能要求原文 → 素材条目（按行拆分，去掉行首序号） */
  private List<String> splitMaterials(String raw) {
    List<String> items = new ArrayList<>();
    if (raw == null || raw.isBlank()) {
      return items;
    }
    for (String line : raw.split("\\n")) {
      String s = line.trim().replaceAll("^\\d{1,2}(\\.\\d{1,2}){0,2}\\s*\\.?\\s*", "")
          .replaceAll("^[（(]\\d{1,2}[)）]\\s*", "").trim();
      if (!s.isEmpty()) {
        items.add(s);
      }
    }
    return items;
  }

  // ============================================================
  // 配置解析 / 留痕 / 查找
  // ============================================================

  private String readModelCode(Long tenantId) {
    List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getCode, DICT_MODEL_CODE)
        .eq(DictEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (!dicts.isEmpty() && dicts.get(0).getValue() != null && !dicts.get(0).getValue().isBlank()) {
      return dicts.get(0).getValue().trim();
    }
    return DEFAULT_MODEL_CODE;
  }

  private ExamFaqModelConfigEntity resolveModelConfig(Long tenantId) {
    try {
      return modelConfigService.findByModelCode(tenantId, readModelCode(tenantId));
    } catch (Exception e) {
      log.warn("[细目表] 模型配置解析失败：tenantId={} error={}", tenantId, e.getMessage());
      return null;
    }
  }

  private void writeDraft(Long sectionNodeId, String prompt, String resultText, String modelName,
      Long tenantId, String dimension) {
    try {
      AiGenerationDraftEntity draft = new AiGenerationDraftEntity();
      draft.setTenantId(tenantId);
      draft.setDraftType(DRAFT_TYPE);
      draft.setSourceType("exam_standard_node_" + dimension);
      draft.setSourceId(sectionNodeId);
      draft.setPrompt(prompt);
      draft.setResultText(resultText);
      draft.setModelName(modelName);
      draft.setStatus("generated");
      draft.setDeleted(0);
      draftMapper.insert(draft);
    } catch (Exception e) {
      log.warn("[细目表] LLM 留痕失败（不影响主链路）：nodeId={} error={}", sectionNodeId, e.getMessage());
    }
  }

  private ExamStandardTaskEntity findTask(Long taskId, Long tenantId) {
    ExamStandardTaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<ExamStandardTaskEntity>()
        .eq(ExamStandardTaskEntity::getTenantId, tenantId)
        .eq(ExamStandardTaskEntity::getId, taskId));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "解析任务不存在");
    }
    return task;
  }

  private ExamStandardNodeEntity findNode(Long nodeId, Long tenantId) {
    ExamStandardNodeEntity node = nodeMapper.selectOne(new LambdaQueryWrapper<ExamStandardNodeEntity>()
        .eq(ExamStandardNodeEntity::getTenantId, tenantId)
        .eq(ExamStandardNodeEntity::getId, nodeId));
    if (node == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "节点不存在");
    }
    return node;
  }

  private static String truncate(String s, int max) {
    return s.length() <= max ? s : s.substring(0, max);
  }
}
