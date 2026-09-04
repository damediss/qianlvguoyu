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
import java.util.regex.Pattern;
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
  /** 理论通道每张细目表（等级×工种）考评点总数目标下限：五/四/三级 200 个（国标：不少于 200 个） */
  static final int THEORY_TOTAL_TARGET_JUNIOR = 200;
  /** 理论通道总数目标下限：二/一级 120 个（国标：不少于 120 个） */
  static final int THEORY_TOTAL_TARGET_SENIOR = 120;
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
        StandardParseApplicationService.STATUS_CONFIRMED,
        StandardParseApplicationService.STATUS_EXPORTED).contains(task.getStatus())) {
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

  /** 按等级取理论考评点总数目标下限：levelCode 0~2（五/四/三级）→ 200；3~4（二/一级）→ 120 */
  public static int theoryTotalTarget(int levelCode) {
    return levelCode >= 3 ? THEORY_TOTAL_TARGET_SENIOR : THEORY_TOTAL_TARGET_JUNIOR;
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
        s -> splitMaterials(s.getRelatedKnowledge()).size(),
        theoryTotalTarget(l2.getLevelCode())).getOrDefault(l2.getId(), 1);
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

      // 按「等级×工种」分配每个小节的理论考评点目标数（国标下限：五至三级 200 点、二至一级 120 点，按权重分配）
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
            s -> splitMaterials(s.getRelatedKnowledge()).size(), theoryTotalTarget(levelCode));
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

      // 数量兜底（国标下限优先：五至三级 200、二至一级 120）：实际不足由 LLM 补点
      topUpTheoryLeaves(task, sections, theoryTargets, config, tenantId, userId, importants);

      // 全表后处理：X/Y/Z 比例硬护栏（超比例自动归 X）+ 同级重复/跨级异名告警
      postProcessTheoryLeaves(taskId);

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

  // ============================================================
  // 全表后处理（Worker 线程末段，TenantContextHolder 已 set）
  // ============================================================

  /**
   * 数量兜底（国标下限优先，2026-09-03 拍板：与去重/比例等规则冲突时优先保证总数下限）：
   * 每张「等级×工种」理论表实际考评点不足目标下限（五至三级 200、二至一级 120）时由 LLM 补点。
   * 缺口先按小节缺口（目标−实际）降序分配；仍有富余再按小节目标降序每次 2 点追补两轮；
   * 已有标题喂进 prompt 并在落库前过滤，防与既有点重复。
   */
  private void topUpTheoryLeaves(ExamStandardTaskEntity task, List<ExamStandardNodeEntity> sections,
      Map<Long, Integer> theoryTargets, ExamFaqModelConfigEntity config,
      Long tenantId, Long userId, List<String> importants) {
    // 本任务全部理论叶按小节统计实际数量
    List<ExamStandardNodeEntity> leaves = nodeMapper.selectList(
        new LambdaQueryWrapper<ExamStandardNodeEntity>()
            .eq(ExamStandardNodeEntity::getTaskId, task.getId())
            .eq(ExamStandardNodeEntity::getNodeLevel, 3)
            .eq(ExamStandardNodeEntity::getDimension, "theory"));
    Map<Long, Integer> actualBySection = new HashMap<>();
    for (ExamStandardNodeEntity leaf : leaves) {
      actualBySection.merge(leaf.getParentId(), 1, Integer::sum);
    }
    Map<String, List<ExamStandardNodeEntity>> sectionsByScope = new LinkedHashMap<>();
    for (ExamStandardNodeEntity l2 : sections) {
      sectionsByScope.computeIfAbsent(l2.getLevelCode() + "|" + l2.getDirection(),
          k -> new ArrayList<>()).add(l2);
    }
    for (Map.Entry<String, List<ExamStandardNodeEntity>> e : sectionsByScope.entrySet()) {
      int levelCode = Integer.parseInt(e.getKey().split("\\|", 2)[0]);
      int target = theoryTotalTarget(levelCode);
      int actual = 0;
      for (ExamStandardNodeEntity l2 : e.getValue()) {
        actual += actualBySection.getOrDefault(l2.getId(), 0);
      }
      int deficit = target - actual;
      if (deficit <= 0) {
        continue;
      }
      log.info("[细目表] 数量兜底：taskId={} scope={} 目标={} 实际={} 需补={}",
          task.getId(), e.getKey(), target, actual, deficit);
      // 第一遍：小节缺口（目标−实际）降序，缺多少补多少
      List<ExamStandardNodeEntity> ordered = new ArrayList<>(e.getValue());
      ordered.sort(Comparator.comparingInt((ExamStandardNodeEntity l2) ->
          theoryTargets.getOrDefault(l2.getId(), 1)
              - actualBySection.getOrDefault(l2.getId(), 0)).reversed());
      int remaining = deficit;
      for (ExamStandardNodeEntity l2 : ordered) {
        if (remaining <= 0) {
          break;
        }
        int gap = theoryTargets.getOrDefault(l2.getId(), 1)
            - actualBySection.getOrDefault(l2.getId(), 0);
        if (gap <= 0) {
          continue;
        }
        int added = topUpSection(task, l2, Math.min(gap, remaining), config, tenantId, userId,
            importants);
        remaining -= added;
        actualBySection.merge(l2.getId(), added, Integer::sum);
      }
      // 第二遍：部分小节超额完成导致按缺口分不满，按小节目标降序每次补 2 点，最多两轮
      if (remaining > 0) {
        ordered.sort(Comparator.comparingInt((ExamStandardNodeEntity l2) ->
            theoryTargets.getOrDefault(l2.getId(), 1)).reversed());
        for (int round = 0; round < 2 && remaining > 0; round++) {
          for (ExamStandardNodeEntity l2 : ordered) {
            if (remaining <= 0) {
              break;
            }
            int added = topUpSection(task, l2, Math.min(2, remaining), config, tenantId, userId,
                importants);
            remaining -= added;
            actualBySection.merge(l2.getId(), added, Integer::sum);
            if (added == 0) {
              break; // LLM 补不出（去重全灭/调用失败）就放弃本轮，避免空转
            }
          }
        }
      }
      if (remaining > 0) {
        log.warn("[细目表] 数量兜底后仍不足：taskId={} scope={} 目标={} 仍缺={}",
            task.getId(), e.getKey(), target, remaining);
        appendTaskWarnings(task.getId(), List.of("LLM护栏：" + scopeLabel(e.getKey()) + " 表补足后仍不足 "
            + target + " 点（缺 " + remaining + " 点），请人工补充或对缺口小节重新概括"));
      }
    }
  }

  /** 单小节补点：LLM 追加生成 topupCount 个不与既有点重复的考评点，返回实际落库数 */
  private int topUpSection(ExamStandardTaskEntity task, ExamStandardNodeEntity l2, int topupCount,
      ExamFaqModelConfigEntity config, Long tenantId, Long userId, List<String> importants) {
    List<ExamStandardNodeEntity> existing = nodeMapper.selectList(
        new LambdaQueryWrapper<ExamStandardNodeEntity>()
            .eq(ExamStandardNodeEntity::getParentId, l2.getId())
            .eq(ExamStandardNodeEntity::getDimension, "theory"));
    List<String> existingTitles = new ArrayList<>();
    for (ExamStandardNodeEntity leaf : existing) {
      existingTitles.add(leaf.getTitle());
    }
    List<String> materials = splitMaterials(l2.getRelatedKnowledge());
    String systemPrompt = buildSystemPrompt(task, l2);
    String userPrompt = buildTopupUserPrompt(sectionPath(task, l2), materials, existingTitles,
        importants, topupCount);
    int maxTokens = computeTheoryTokens(topupCount);
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
        result = parsePointsResponse(content, topupCount, false);
        if (!result.getPoints().isEmpty()) {
          break;
        }
        lastError = "points 为空";
        result = null;
      } catch (Exception ex) {
        lastError = ex.getMessage();
        log.warn("[细目表] 补点第 {} 次尝试失败：taskId={} node={} error={}",
            attempt, task.getId(), l2.getNodeKey(), lastError);
      }
    }
    writeDraft(l2.getId(), systemPrompt + "\n" + userPrompt, lastContent != null ? lastContent : "",
        config.getModelId(), tenantId, "theory");
    if (result == null) {
      return 0;
    }
    List<StandardLlmPoint> points = filterAgainstExisting(
        dedupSectionPoints(clampImportants(result.getPoints(), importants)), existingTitles);
    int added = 0;
    for (StandardLlmPoint point : points) {
      insertLeaf(l2, "theory", point.title(), point.important(), "llm", added, tenantId);
      added++;
    }
    return added;
  }

  /** 补点过滤：与已有考评点相同/相近（规范化后相等或互为子串，≥2 字）的丢弃 */
  public static List<StandardLlmPoint> filterAgainstExisting(List<StandardLlmPoint> points,
      List<String> existingTitles) {
    List<String> norms = new ArrayList<>();
    for (String t : existingTitles) {
      String n = normalizeLeafTitle(t);
      if (n.length() >= 2) {
        norms.add(n);
      }
    }
    List<StandardLlmPoint> out = new ArrayList<>(points.size());
    for (StandardLlmPoint p : points) {
      String norm = normalizeLeafTitle(p.title());
      boolean dup = norm.length() >= 2 && norms.stream()
          .anyMatch(e -> e.equals(norm) || e.contains(norm) || norm.contains(e));
      if (dup) {
        log.warn("[细目表] 补点与已有考评点相同/相近，丢弃：{}", p.title());
        continue;
      }
      out.add(p);
    }
    return out;
  }

  /**
   * 理论叶全表后处理：
   * 1) X/Y/Z 比例硬护栏（国标：X≥85%、Y≤10%、Z≤5%）——每张「等级×工种」表内超出档位的
   *    LLM pending 草稿叶自动归入 X（从树序末尾往前调；人工/已审叶不动，仍超额则告警）；
   * 2) 同级跨小节重复、跨级疑似同点异名 → 告警追加到任务 parseWarnings（前端任务详情可见）。
   */
  private void postProcessTheoryLeaves(Long taskId) {
    List<ExamStandardNodeEntity> leaves = nodeMapper.selectList(
        new LambdaQueryWrapper<ExamStandardNodeEntity>()
            .eq(ExamStandardNodeEntity::getTaskId, taskId)
            .eq(ExamStandardNodeEntity::getNodeLevel, 3)
            .eq(ExamStandardNodeEntity::getDimension, "theory")
            .orderByAsc(ExamStandardNodeEntity::getNodeKey));
    if (leaves.isEmpty()) {
      return;
    }
    // 按 等级×工种 分组（nodeKey 升序保证组内树序）
    Map<String, List<ExamStandardNodeEntity>> byScope = new LinkedHashMap<>();
    for (ExamStandardNodeEntity leaf : leaves) {
      byScope.computeIfAbsent(leaf.getLevelCode() + "|" + leaf.getDirection(),
          k -> new ArrayList<>()).add(leaf);
    }
    List<String> warnings = new ArrayList<>();
    for (Map.Entry<String, List<ExamStandardNodeEntity>> e : byScope.entrySet()) {
      String label = scopeLabel(e.getKey());
      enforceImportantRatios(label, e.getValue(), warnings);
      warnSameScopeDuplicates(label, e.getValue(), warnings);
    }
    warnCrossLevelNameDrift(byScope, warnings);
    if (warnings.isEmpty()) {
      return;
    }
    log.info("[细目表] 全表后处理告警 {} 条：taskId={}", warnings.size(), taskId);
    appendTaskWarnings(taskId, warnings);
  }

  private String scopeLabel(String scopeKey) {
    String[] parts = scopeKey.split("\\|", 2);
    return levelName(Integer.parseInt(parts[0])) + "/" + parts[1];
  }

  /** 单张表 X/Y/Z 比例护栏：Y≤10%、Z≤5%（两者压降后 X 自然 ≥85%），超额的 LLM pending 叶自动归 X */
  private void enforceImportantRatios(String scopeLabel, List<ExamStandardNodeEntity> scopeLeaves,
      List<String> warnings) {
    int n = scopeLeaves.size();
    int allowedY = (int) Math.floor(n * 0.10);
    int allowedZ = (int) Math.floor(n * 0.05);
    int yCount = 0;
    int zCount = 0;
    for (ExamStandardNodeEntity leaf : scopeLeaves) {
      if ("Y".equals(leaf.getImportant())) {
        yCount++;
      } else if ("Z".equals(leaf.getImportant())) {
        zCount++;
      }
    }
    int flippedY = flipExcessImportant(scopeLeaves, "Y", yCount - allowedY);
    int flippedZ = flipExcessImportant(scopeLeaves, "Z", zCount - allowedZ);
    if (flippedY + flippedZ > 0) {
      log.info("[细目表] X/Y/Z 比例护栏：{} 总数={} Y:{}→{} Z:{}→{}（超额自动归 X）",
          scopeLabel, n, yCount, yCount - flippedY, zCount, zCount - flippedZ);
    }
    int remainY = yCount - flippedY - allowedY;
    int remainZ = zCount - flippedZ - allowedZ;
    if (remainY > 0 || remainZ > 0) {
      warnings.add("LLM护栏：" + scopeLabel + " 表 Y/Z 仍超比例（Y 超 " + Math.max(remainY, 0)
          + " 个、Z 超 " + Math.max(remainZ, 0) + " 个，涉及人工或已审叶未自动调整），请人工处理");
    }
  }

  /** 从树序末尾往前把超额档位的 LLM pending 草稿叶改为 X，返回实际调整数 */
  private int flipExcessImportant(List<ExamStandardNodeEntity> sortedLeaves, String from, int excess) {
    int flipped = 0;
    for (int i = sortedLeaves.size() - 1; i >= 0 && flipped < excess; i--) {
      ExamStandardNodeEntity leaf = sortedLeaves.get(i);
      if (!from.equals(leaf.getImportant())) {
        continue;
      }
      if (!"llm".equals(leaf.getSource()) || !"pending".equals(leaf.getReviewStatus())) {
        continue;
      }
      ExamStandardNodeEntity update = new ExamStandardNodeEntity();
      update.setId(leaf.getId());
      update.setImportant("X");
      nodeMapper.updateById(update);
      leaf.setImportant("X");
      flipped++;
    }
    return flipped;
  }

  /** 同级跨小节重复告警：规范化标题相等或互为子串（≥2 字）；同小节内重复已在生成时自动合并 */
  private void warnSameScopeDuplicates(String scopeLabel, List<ExamStandardNodeEntity> scopeLeaves,
      List<String> warnings) {
    List<String[]> seen = new ArrayList<>();
    for (ExamStandardNodeEntity leaf : scopeLeaves) {
      String norm = normalizeLeafTitle(leaf.getTitle());
      if (norm.length() < 2) {
        continue;
      }
      for (String[] prev : seen) {
        if (prev[0].equals(norm) || prev[0].contains(norm) || norm.contains(prev[0])) {
          warnings.add("LLM告警：" + scopeLabel + " 疑似重复考评点「" + leaf.getTitle() + "」（"
              + leaf.getNodeKey() + "）与「" + prev[1] + "」（" + prev[2] + "），同级内不应重复出现");
          break;
        }
      }
      seen.add(new String[]{norm, leaf.getTitle(), leaf.getNodeKey()});
    }
  }

  /** 跨级疑似同点异名告警：同工种不同等级间规范化标题互为子串但不一致（≥4 字降噪） */
  private void warnCrossLevelNameDrift(Map<String, List<ExamStandardNodeEntity>> byScope,
      List<String> warnings) {
    Map<String, Map<Integer, List<String>>> titlesByDirection = new LinkedHashMap<>();
    for (Map.Entry<String, List<ExamStandardNodeEntity>> e : byScope.entrySet()) {
      String[] parts = e.getKey().split("\\|", 2);
      int levelCode = Integer.parseInt(parts[0]);
      Map<Integer, List<String>> levelTitles = titlesByDirection
          .computeIfAbsent(parts[1], k -> new LinkedHashMap<>());
      List<String> titles = levelTitles.computeIfAbsent(levelCode, k -> new ArrayList<>());
      for (ExamStandardNodeEntity leaf : e.getValue()) {
        titles.add(leaf.getTitle());
      }
    }
    for (Map.Entry<String, Map<Integer, List<String>>> dir : titlesByDirection.entrySet()) {
      List<Integer> levels = new ArrayList<>(dir.getValue().keySet());
      for (int i = 0; i < levels.size(); i++) {
        for (int j = i + 1; j < levels.size(); j++) {
          for (String t1 : dir.getValue().get(levels.get(i))) {
            String n1 = normalizeLeafTitle(t1);
            if (n1.length() < 4) {
              continue;
            }
            for (String t2 : dir.getValue().get(levels.get(j))) {
              String n2 = normalizeLeafTitle(t2);
              if (n2.length() < 4 || n1.equals(n2)) {
                continue;
              }
              if (n1.contains(n2) || n2.contains(n1)) {
                warnings.add("LLM告警：疑似同一考评点跨级别名称不一致——" + levelName(levels.get(i))
                    + "「" + t1 + "」与 " + levelName(levels.get(j)) + "「" + t2 + "」（"
                    + dir.getKey() + "），跨级别出现时名称应一致");
              }
            }
          }
        }
      }
    }
  }

  /** 告警追加到任务 parseWarnings（与解析告警同栏展示）；已存在的行不重复追加，总量封顶 100 行 */
  private void appendTaskWarnings(Long taskId, List<String> warnings) {
    ExamStandardTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      return;
    }
    String existing = task.getParseWarnings() != null ? task.getParseWarnings() : "";
    StringBuilder sb = new StringBuilder(existing);
    int added = 0;
    for (String w : warnings) {
      if (added >= 100) {
        sb.append("LLM告警：……其余 ").append(warnings.size() - added).append(" 条详见后端日志\n");
        break;
      }
      if (!existing.contains(w)) {
        sb.append(w).append('\n');
        added++;
      }
    }
    if (added == 0) {
      return;
    }
    ExamStandardTaskEntity update = new ExamStandardTaskEntity();
    update.setId(taskId);
    update.setParseWarnings(sb.toString().trim());
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
    // 理论通道：越档产出归入最低允许档（不丢弃考评点，防空结果）；
    // 同小节内相同/相近考评点自动合并（国标：内容相同或相近的考评点不重复出现）
    List<StandardLlmPoint> points = skill ? result.getPoints()
        : dedupSectionPoints(clampImportants(result.getPoints(), importants));
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

  /** 考评点标题规范化：去全部空白与标点（含全角），用于相同/相近判定 */
  static String normalizeLeafTitle(String title) {
    if (title == null) {
      return "";
    }
    return TITLE_NOISE.matcher(title).replaceAll("");
  }

  private static final Pattern TITLE_NOISE =
      Pattern.compile("[\\s\\p{P}]+", Pattern.UNICODE_CHARACTER_CLASS);

  /**
   * 同小节内相同/相近考评点自动合并：标题规范化后相等或互为子串即视为相近，保留首个、丢弃后续。
   * 仅当两侧规范化标题均 ≥2 字时才做子串判定，避免单字标题误并。
   */
  public static List<StandardLlmPoint> dedupSectionPoints(List<StandardLlmPoint> points) {
    List<StandardLlmPoint> out = new ArrayList<>(points.size());
    List<String> norms = new ArrayList<>(points.size());
    for (StandardLlmPoint p : points) {
      String norm = normalizeLeafTitle(p.title());
      boolean dup = false;
      if (norm.length() >= 2) {
        for (String existing : norms) {
          if (existing.length() >= 2
              && (existing.equals(norm) || existing.contains(norm) || norm.contains(existing))) {
            dup = true;
            break;
          }
        }
      }
      if (dup) {
        log.warn("[细目表] 同小节相同/相近考评点自动合并，丢弃：{}", p.title());
        continue;
      }
      norms.add(norm);
      out.add(p);
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
          .append("3. 考评点须符合国家职业标准，概念清晰、表达准确，各考评点之间相互独立、均可独立命题考核；\n")
          .append("4. 考评点名称与所用术语须符合国家有关标准或规定；"
              + "同一小节内不得出现内容相同或相近的考评点；\n")
          .append("5. 全表重要程度比例要求 X 占 85% 以上、Y 不超过 10%、Z 不超过 5%，请从严控制 Y、Z 档；\n")
          .append("6. 以 JSON 输出（必须含 \"points\" 数组），格式：")
          .append("{\"points\":[{\"title\":\"…\",\"important\":\"X\"}]}");
    }
    return sb.toString();
  }

  /**
   * 理论通道每小节目标点数分配（国标下限：五至三级每等级 ≥200 点、二至一级 ≥120 点，按权重分配，
   * 优先级高于材料条数规则）。
   *
   * <p>权重来源优先级：小节手工权重（绝对值，直接 ×totalTarget）&gt; L1 权重 × 小节材料条数占比
   * &gt; L1 无权重时按材料条数在全表剩余预算内兜底。最大余数法取整使合计严格等于 totalTarget；
   * 每小节保底 1 点（保底造成的总量略超下限允许）。
   *
   * @param l1Nodes       该「等级×工种」范围内的 L1 节点（取 weight，可空元素忽略）
   * @param sections      该范围内的 L2 小节
   * @param materialCount 小节材料条数函数（空小节应按 1 条保底，由调用方保证 ≥0）
   * @param totalTarget   本等级全表考评点总数目标（五至三级 200、二至一级 120，见 {@link #theoryTotalTarget}）
   * @return 小节 id → 目标点数（≥1）
   */
  public static Map<Long, Integer> allocateTheoryTargets(List<ExamStandardNodeEntity> l1Nodes,
      List<ExamStandardNodeEntity> sections, ToIntFunction<ExamStandardNodeEntity> materialCount,
      int totalTarget) {
    BigDecimal total = BigDecimal.valueOf(totalTarget);
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

    // 小数目标：手工权重的小节直接 totalTarget×w；其余先在所属 L1 预算内按材料占比分，L1 无权重进兜底池
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

    // 最大余数法取整：合计严格等于 totalTarget（有小数预算时）
    Map<Long, Integer> targets = largestRemainder(decimal, totalTarget);
    // 每小节保底 1 点（保底造成的总量略超下限允许）
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

  /** 补点 user prompt：在已有考评点之外追加 N 个，已有标题列出防重复（C2：路径+素材在前，要求在后） */
  private static String buildTopupUserPrompt(String sectionPath, List<String> materials,
      List<String> existingTitles, List<String> importants, int topupCount) {
    StringBuilder sb = new StringBuilder();
    sb.append("小节路径：").append(sectionPath).append('\n');
    sb.append("原文素材：\n");
    if (materials.isEmpty()) {
      sb.append("（本小节无列举项，请依据小节主题与职业标准常识概括）\n");
    } else {
      for (int i = 0; i < materials.size(); i++) {
        sb.append("（").append(i + 1).append("）").append(materials.get(i)).append('\n');
      }
    }
    sb.append("已有考评点（不得与其中任何一条相同或相近）：\n");
    for (String t : existingTitles) {
      sb.append("· ").append(t).append('\n');
    }
    sb.append("要求（理论知识考评点补充）：\n");
    sb.append("1. 在已有考评点之外再补充 ").append(topupCount)
        .append(" 个考评点（数量必须严格遵守），优先覆盖原文素材中尚未命中的知识小点，"
            + "每个为 ≤20 字的名词性短语，不得照抄长句；\n");
    sb.append("2. ").append(theoryImportantRule(importants)).append("；\n")
        .append("3. 考评点须符合国家职业标准，概念清晰、表达准确，各考评点之间相互独立、均可独立命题考核；\n")
        .append("4. 考评点名称与所用术语须符合国家有关标准或规定；\n")
        .append("5. 以 JSON 输出（必须含 \"points\" 数组），格式：")
        .append("{\"points\":[{\"title\":\"…\",\"important\":\"X\"}]}");
    return sb.toString();
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
