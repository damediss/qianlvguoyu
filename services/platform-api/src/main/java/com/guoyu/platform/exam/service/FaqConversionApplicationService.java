package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.event.FaqEnhancementStartEvent;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.infrastructure.FaqLlmClientException;
import com.guoyu.platform.ai.infrastructure.FaqLlmException;
import com.guoyu.platform.ai.infrastructure.FaqLlmPartialResultException;
import com.guoyu.platform.ai.infrastructure.FaqLlmParseException;
import com.guoyu.platform.ai.infrastructure.FaqLlmRateLimitException;
import com.guoyu.platform.ai.infrastructure.FaqLlmResponseParser;
import com.guoyu.platform.ai.infrastructure.FaqLlmResponseParser.MergedResult;
import com.guoyu.platform.ai.infrastructure.FaqLlmServerException;
import com.guoyu.platform.ai.infrastructure.FaqLlmTimeoutException;
import com.guoyu.platform.ai.infrastructure.FaqLlmTruncatedException;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.dto.FaqConversionQueryReq;
import com.guoyu.platform.exam.dto.FaqConversionSubmitReq;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamFaqConversionBatchEntity;
import com.guoyu.platform.exam.entity.ExamFaqConversionEntity;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamFaqConversionMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.vo.FaqBatchVO;
import com.guoyu.platform.exam.vo.FaqConversionVO;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * FAQ 转换编排服务 — 读题 → 调 LLM → 入库 → 更新进度。
 *
 * <p>重构说明（2026-07-20）：
 * <ul>
 *   <li>批次内并行化：单线程 for 循环 → Semaphore 限流的 CompletableFuture 分块提交</li>
 *   <li>LLM 调用合并：2 次串行（similar + negative）→ 1 次合并 prompt</li>
 *   <li>异常分类 + 指数退避 + 熔断器</li>
 *   <li>连接池化（FaqLlmClient 内部实现）</li>
 * </ul>
 *
 * <p>注意：异步处理时，tenantId 由 HTTP 线程捕获后通过参数传递，
 * 整个异步调用链不调 SecurityContext。
 */
@Service
@Slf4j
public class FaqConversionApplicationService {

  // 可转换题型
  private static final Set<String> CONVERTIBLE_TYPES = Set.of("single", "multi", "judge");

  private final ExamExamDetailMapper examDetailMapper;
  private final ExamWorkTypeMapper workTypeMapper;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final ExamFaqConversionMapper faqConversionMapper;
  private final FaqConversionBatchService batchService;
  private final FaqModelConfigApplicationService modelConfigService;
  private final ExamFaqConverter faqConverter;
  private final KnowledgeStructurePathResolver pathResolver;
  private final FaqLlmPromptBuilder promptBuilder;
  private final FaqLlmClient llmClient;
  private final FaqLlmResponseParser llmParser;
  private final ObjectMapper objectMapper;
  private final ThreadPoolTaskExecutor faqConversionExecutor;
  private final ThreadPoolTaskExecutor faqRecordExecutor;
  private final ApplicationEventPublisher eventPublisher;

  public FaqConversionApplicationService(
      ExamExamDetailMapper examDetailMapper,
      ExamWorkTypeMapper workTypeMapper,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      ExamFaqConversionMapper faqConversionMapper,
      FaqConversionBatchService batchService,
      FaqModelConfigApplicationService modelConfigService,
      ExamFaqConverter faqConverter,
      KnowledgeStructurePathResolver pathResolver,
      FaqLlmPromptBuilder promptBuilder,
      FaqLlmClient llmClient,
      FaqLlmResponseParser llmParser,
      ObjectMapper objectMapper,
      @Qualifier("faqConversionExecutor") ThreadPoolTaskExecutor faqConversionExecutor,
      @Qualifier("faqRecordExecutor") ThreadPoolTaskExecutor faqRecordExecutor,
      ApplicationEventPublisher eventPublisher) {
    this.examDetailMapper = examDetailMapper;
    this.workTypeMapper = workTypeMapper;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.faqConversionMapper = faqConversionMapper;
    this.batchService = batchService;
    this.modelConfigService = modelConfigService;
    this.faqConverter = faqConverter;
    this.pathResolver = pathResolver;
    this.promptBuilder = promptBuilder;
    this.llmClient = llmClient;
    this.llmParser = llmParser;
    this.objectMapper = objectMapper;
    this.faqConversionExecutor = faqConversionExecutor;
    this.faqRecordExecutor = faqRecordExecutor;
    this.eventPublisher = eventPublisher;
  }

  /**
   * 发起 FAQ 转换（HTTP 请求线程入口）。
   */
  @Transactional
  public FaqBatchVO submitConversion(FaqConversionSubmitReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    // 校验工种
    ExamWorkTypeEntity workType = workTypeMapper.selectById(req.workTypeId());
    if (workType == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "工种不存在");
    }

    // 校验模型配置
    ExamFaqModelConfigEntity modelConfig = modelConfigService.findByModelCode(tenantId, req.modelCode());
    if (modelConfig == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在或未启用");
    }

    // 查询符合条件的试题
    List<String> types = (req.questionTypes() != null && !req.questionTypes().isEmpty())
        ? req.questionTypes() : List.of("single", "multi", "judge");
    List<ExamExamDetailEntity> exams = queryConvertibleExams(
        tenantId, req.workTypeId(), req.courseIds(), types);

    if (exams.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有符合条件的试题可转换");
    }

    // 创建批次
    ExamFaqConversionBatchEntity batch = batchService.createBatch(
        tenantId, userId, req.workTypeId(), workType.getTitle(),
        req.modelCode(), modelConfig.getModelName(),
        exams.size(), req.concurrencyLimit());

    // 创建转换明细记录 + 规则生成 standard_question / answers / tag_name
    List<ExamFaqConversionEntity> conversionEntities = new ArrayList<>();
    for (ExamExamDetailEntity exam : exams) {
      ExamFaqConversionEntity conv = createConversionRecord(
          tenantId, userId, batch.getId(), exam, req.modelCode(), req.workTypeId());
      conversionEntities.add(conv);
    }
    // 批量插入转换明细
    for (ExamFaqConversionEntity conv : conversionEntities) {
      faqConversionMapper.insert(conv);
    }

    log.info("FAQ 转换批次已创建并写入明细：batchId={}, examCount={}", batch.getId(), exams.size());

    // 所有批次统一走异步 LLM 增强，HTTP 立即返回 batchId，前端轮询进度。
    // 异步在事务成功提交后由 @TransactionalEventListener(AFTER_COMMIT) 启动，
    // 避免事务未提交时异步线程查询不到 pending 记录。
    Long batchId = batch.getId();
    eventPublisher.publishEvent(new FaqEnhancementStartEvent(batchId, tenantId, userId, req.modelCode()));

    return FaqBatchVO.from(batch);
  }

  /**
   * LLM 增强 — 异步执行（CompletableFuture 分块并行）。
   * 注意：此方法在编排线程（faqConversionExecutor）中执行，不能调 SecurityContext；
   * 租户上下文在编排线程整个生命周期内保持，供 MyBatis-Plus 多租户拦截器使用。
   */
  public void processLlmEnhancementAsync(Long batchId, Long tenantId, Long userId, String modelCode) {
    log.info("FAQ LLM 异步增强开始：batchId={}, tenantId={}, modelCode={}", batchId, tenantId, modelCode);

    TenantContextHolder.setTenantId(tenantId);
    try {
      // 编排线程：一次性加载 pending 记录 + 模型配置 + 批次信息 + 知识点缓存
      List<ExamFaqConversionEntity> pending = loadPendingRecords(batchId);
      ExamFaqModelConfigEntity modelConfig = modelConfigService.findByModelCode(tenantId, modelCode);
      if (modelConfig == null) {
        log.error("模型配置不存在：tenantId={}, modelCode={}", tenantId, modelCode);
        return;
      }
      ExamFaqConversionBatchEntity batch = batchService.getBatchEntity(tenantId, batchId);
      int concurrencyLimit = (batch != null && batch.getConcurrencyLimit() != null)
          ? batch.getConcurrencyLimit() : 12; // 默认 12
      Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap = loadKnowledgeMap(tenantId);

      // 熔断器：批次级局部变量（每个批次一个实例，所有并发线程共享）
      CircuitBreaker circuitBreaker = new CircuitBreaker(5);
      Semaphore semaphore = new Semaphore(concurrencyLimit);

      // 分块提交：chunkSize 独立于 concurrencyLimit，确保 semaphore 始终饱和。
      // 暂停通过双重检查生效：块间检查 + 逐条检查，
      // 排队的任务在获取许可后感知暂停立即退出，仅当前在跑的 LLM 调用跑完。
      int chunkSize = 50;
      for (int offset = 0; offset < pending.size(); offset += chunkSize) {
        if (!batchService.isRunning(tenantId, batchId)) {
          log.info("批次已暂停，停止提交新任务：batchId={}, offset={}", batchId, offset);
          break;
        }
        List<ExamFaqConversionEntity> chunk = pending.subList(offset, Math.min(offset + chunkSize, pending.size()));
        List<CompletableFuture<Void>> futures = chunk.stream()
            .map(record -> CompletableFuture.runAsync(() -> {
              // ⚠️ 关键：每条记录任务内部必须独立设置租户上下文（ThreadLocal 不跨线程传播）
              TenantContextHolder.setTenantId(tenantId);
              try {
                // 秒级暂停响应：每条记录开头再检查一次
                if (!batchService.isRunning(tenantId, batchId)) {
                  return; // 记录保持 pending，resume 时续跑
                }
                processRecordWithPermit(record, semaphore, modelConfig, tenantId, batchId, circuitBreaker, knowledgeMap);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                TenantContextHolder.clear();
              }
            }, faqRecordExecutor))
            .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      }

      log.info("FAQ LLM 异步增强完成：batchId={}", batchId);
      checkBatchCompletion(batchId, tenantId);
    } finally {
      TenantContextHolder.clear();
    }
  }

  /**
   * 带信号量许可的单条记录处理（许可获取/释放与重试在同一方法内闭环）。
   */
  private void processRecordWithPermit(ExamFaqConversionEntity record, Semaphore semaphore,
                                       ExamFaqModelConfigEntity modelConfig,
                                       Long tenantId, Long batchId, CircuitBreaker circuitBreaker,
                                       Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap)
      throws InterruptedException {
    semaphore.acquire();
    try {
      processSingleRecordMerged(record, tenantId, modelConfig, batchId, circuitBreaker, knowledgeMap);
    } finally {
      semaphore.release();
    }
  }

  /**
   * 对单条记录执行合并 LLM 增强（一次调用生成 similar + negative），含指数退避重试。
   * 异常分类由 FaqLlmClient 抛出，本法只负责区分瞬时/确定性的处理策略。
   */
  private void processSingleRecordMerged(ExamFaqConversionEntity entity, Long tenantId,
                                         ExamFaqModelConfigEntity modelConfig, Long batchId,
                                         CircuitBreaker circuitBreaker,
                                         Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap) {
    if ("skipped".equals(entity.getConversionStatus())) {
      return;
    }

    // 标记为转换中
    entity.setConversionStatus("processing");
    faqConversionMapper.updateById(entity);

    String lastError = null;
    try {
      // 准备 prompt 上下文
      String knowledgePath = pathResolver.resolveLastThreeLevels(entity.getKnowledgeId(), knowledgeMap);
      Map<String, String> promptCtx = faqConverter.buildLlmPromptContext(toExamDetail(entity), knowledgePath);

      // 一次 LLM 调用生成 similar + negative
      // system 放稳定前缀（DeepSeek 上下文缓存），user 放题目变量
      String systemPrompt = promptBuilder.buildSystemPrompt();
      String userPrompt = promptBuilder.buildMergedUserPrompt(promptCtx);
      String rawContent = callLlmWithRetry(
          systemPrompt, userPrompt, modelConfig, tenantId, circuitBreaker);

      // 解析合并响应
      MergedResult result = llmParser.extractMergedResult(rawContent);
      if (result == null) {
        // JSON 解析失败（finish_reason='stop' 但格式坏）→ 确定性，不重试
        throw new FaqLlmParseException("JSON 解析失败（finish_reason=stop 但格式异常）");
      }
      if (result.hasEmptyField()) {
        // 部分字段为空数组 → 瞬时，可重试
        throw new FaqLlmPartialResultException(
            "部分字段为空数组（similar=" + result.similarQuestions().size()
                + ", negative=" + result.negativeQuestions().size() + "）");
      }

      // 序列化为 JSON
      entity.setSimilarQuestions(toJson(result.similarQuestions()));
      entity.setNegativeQuestions(toJson(result.negativeQuestions()));
      entity.setConversionStatus("success");
      entity.setErrorMessage(null);
      entity.setUpdatedAt(LocalDateTime.now());
      faqConversionMapper.updateById(entity);

      batchService.incrementSuccess(batchId);
      batchService.decrementPending(batchId);

    } catch (FaqLlmParseException | FaqLlmClientException e) {
      // 确定性失败 → 不重试，直接标 failed
      lastError = e.getMessage();
      log.error("FAQ LLM 确定性失败（不重试）：conversionId={}, error={}", entity.getId(), lastError);
      markRecordFailed(entity, batchId, "确定性失败：" + lastError);
    } catch (FaqLlmException e) {
      // 瞬时失败（已耗尽 3 次重试）→ 标 failed
      lastError = e.getMessage();
      log.error("FAQ LLM 瞬时失败（已重试 3 次）：conversionId={}, error={}", entity.getId(), lastError);
      markRecordFailed(entity, batchId, lastError);
    } catch (Exception e) {
      // 未知异常 → 兜底为瞬时失败
      lastError = e.getMessage();
      log.error("FAQ LLM 未知异常：conversionId={}, error={}", entity.getId(), lastError, e);
      markRecordFailed(entity, batchId, lastError);
    }
  }

  /**
   * LLM 调用（含指数退避重试 + 熔断计数）。
   * 瞬时异常（429/5xx/超时/截断/部分结果）退避重试 3 次；
   * 确定性异常（解析失败/4xx 非 429）不重试直接抛出。
   *
   * <p>对 DeepSeek 启用 JSON Output（jsonMode=true），空 content 由 FaqLlmClient 内部重试 3 次后返回空字符串。
   */
  private String callLlmWithRetry(String systemPrompt, String userPrompt,
                                  ExamFaqModelConfigEntity modelConfig,
                                  Long tenantId, CircuitBreaker circuitBreaker) {
    long[] backoffMs = {2000, 4000, 8000};
    for (int attempt = 0; attempt <= backoffMs.length; attempt++) {
      try {
        String result = llmClient.chatCompletion(
            modelConfig.getBaseUrl(), modelConfig.getApiKey(),
            modelConfig.getModelId(), systemPrompt, userPrompt,
            modelConfig.getMaxTokens(), modelConfig.getThinking(),
            true, tenantId);
        circuitBreaker.onSuccess(); // 成功时复位连续 429 计数
        return result;
      } catch (FaqLlmRateLimitException e) {
        circuitBreaker.onHttp429();
        attemptOrThrow(e, attempt, backoffMs);
      } catch (FaqLlmServerException | FaqLlmTimeoutException
             | FaqLlmTruncatedException | FaqLlmPartialResultException e) {
        circuitBreaker.onSuccess(); // 非 429 结果，复位连续 429 计数
        attemptOrThrow(e, attempt, backoffMs);
      }
      // FaqLlmParseException / FaqLlmClientException：确定性，不重试直接向上抛
    }
    throw new IllegalStateException("不应到达此处");
  }

  /**
   * 退避 sleep（持有许可）或抛出（已达重试上限）。
   */
  private void attemptOrThrow(RuntimeException e, int attempt, long[] backoffMs) {
    if (attempt == backoffMs.length) {
      throw e;
    }
    try {
      Thread.sleep(backoffMs[attempt]); // 持有许可 sleep
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw e;
    }
  }

  /**
   * 标记记录为 failed 并更新计数器。
   */
  private void markRecordFailed(ExamFaqConversionEntity entity, Long batchId, String errorMessage) {
    entity.setConversionStatus("failed");
    entity.setErrorMessage(errorMessage != null
        ? errorMessage.substring(0, Math.min(errorMessage.length(), 1024))
        : "未知错误");
    entity.setUpdatedAt(LocalDateTime.now());
    faqConversionMapper.updateById(entity);
    batchService.incrementFailed(batchId);
    batchService.decrementPending(batchId);
  }

  /**
   * 查询待处理记录（按 batch_id + conversion_status='pending' 排序）。
   * 注意：调用方必须已设置 TenantContextHolder。
   */
  private List<ExamFaqConversionEntity> loadPendingRecords(Long batchId) {
    return faqConversionMapper.selectList(new LambdaQueryWrapper<ExamFaqConversionEntity>()
        .eq(ExamFaqConversionEntity::getBatchId, batchId)
        .eq(ExamFaqConversionEntity::getConversionStatus, "pending")
        .orderByAsc(ExamFaqConversionEntity::getId));
  }

  /**
   * 一次性加载本租户知识点 Map，批次内复用，避免每条记录全表扫描。
   * 注意：调用方必须已设置 TenantContextHolder。
   */
  private Map<Integer, ExamKnowledgeStructureEntity> loadKnowledgeMap(Long tenantId) {
    List<ExamKnowledgeStructureEntity> all = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId.intValue()));
    Map<Integer, ExamKnowledgeStructureEntity> map = new LinkedHashMap<>(all.size());
    for (ExamKnowledgeStructureEntity ks : all) {
      map.put(ks.getId(), ks);
    }
    return map;
  }

  /**
   * 查询可转换试题（单租户、指定工种/科目、已审核通过）。
   */
  private List<ExamExamDetailEntity> queryConvertibleExams(
      Long tenantId, Integer workTypeId, List<Integer> courseIds, List<String> types) {
    LambdaQueryWrapper<ExamExamDetailEntity> wrapper = new LambdaQueryWrapper<ExamExamDetailEntity>()
        .eq(ExamExamDetailEntity::getTenantId, tenantId.intValue())
        .eq(ExamExamDetailEntity::getStatus, 1)
        .eq(ExamExamDetailEntity::getCheckStatus, 1)
        .eq(ExamExamDetailEntity::getFinalCheckStatus, 1)
        .in(ExamExamDetailEntity::getType, types);

    // 如果指定了科目，通过 knowledge_structure 关联过滤
    if (courseIds != null && !courseIds.isEmpty()) {
      List<Integer> knowledgeIds = findKnowledgeIdsByCourses(tenantId, courseIds);
      if (knowledgeIds.isEmpty()) {
        return List.of();
      }
      wrapper.in(ExamExamDetailEntity::getKnowledgeId, knowledgeIds);
    }

    return examDetailMapper.selectList(wrapper);
  }

  private List<Integer> findKnowledgeIdsByCourses(Long tenantId, List<Integer> courseIds) {
    List<ExamKnowledgeStructureEntity> ksList = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId.intValue())
            .in(ExamKnowledgeStructureEntity::getCourseId, courseIds)
            .select(ExamKnowledgeStructureEntity::getId));
    return ksList.stream().map(ExamKnowledgeStructureEntity::getId).toList();
  }

  /**
   * 创建单条转换记录（规则生成部分，不调 LLM）。
   */
  private ExamFaqConversionEntity createConversionRecord(
      Long tenantId, Long userId, Long batchId, ExamExamDetailEntity exam, String modelCode, Integer workTypeId) {
    LocalDateTime now = LocalDateTime.now();
    ExamFaqConversionEntity conv = new ExamFaqConversionEntity();
    conv.setId(IdWorker.getId());
    conv.setTenantId(tenantId);
    conv.setBatchId(batchId);
    conv.setExamId(exam.getId());
    conv.setWorkTypeId(workTypeId);
    conv.setKnowledgeId(exam.getKnowledgeId());
    conv.setSourceType(exam.getType());
    conv.setSourceDifficulty(exam.getDifficulty());
    conv.setSourceContent(exam.getContent());
    conv.setSourceAnswer(exam.getAnswer());
    conv.setConversionStatus("pending");
    conv.setModelCode(modelCode);
    conv.setRetryCount(0);
    conv.setWeknoraImported(0);
    conv.setCreatedBy(userId);
    conv.setUpdatedBy(userId);
    conv.setCreatedAt(now);
    conv.setUpdatedAt(now);
    conv.setDeleted(0);

    // 知识点路径 → tag_name
    String knowledgePath = pathResolver.resolveLastThreeLevels(exam.getKnowledgeId(), tenantId);
    String pathForTag = knowledgePath;
    if (pathForTag == null && exam.getKeyword() != null && !exam.getKeyword().isBlank()) {
      pathForTag = exam.getKeyword().split("\\|")[0];
    }
    String standardQuestion = faqConverter.buildStandardQuestion(exam);
    String answers = faqConverter.buildAnswers(exam);
    String tagName = faqConverter.buildTagName(exam.getType(), pathForTag, exam.getDifficulty());

    conv.setStandardQuestion(standardQuestion);
    conv.setAnswers(answers);
    conv.setTagName(tagName);

    return conv;
  }

  // ──────────────────────── 批次操作 ────────────────────────

  /**
   * 查询批次进度（用于前端轮询）。
   */
  @Transactional(readOnly = true)
  public FaqBatchVO getBatchProgress(Long tenantId, Long batchId) {
    return batchService.getById(tenantId, batchId);
  }

  /**
   * 查询转换记录分页。
   */
  @Transactional(readOnly = true)
  public PageResponse<FaqConversionVO> pageRecords(FaqConversionQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ExamFaqConversionEntity> wrapper = new LambdaQueryWrapper<ExamFaqConversionEntity>()
        .eq(ExamFaqConversionEntity::getTenantId, tenantId)
        .eq(req.batchId() != null, ExamFaqConversionEntity::getBatchId, req.batchId())
        .eq(req.conversionStatus() != null, ExamFaqConversionEntity::getConversionStatus, req.conversionStatus())
        .like(req.tagName() != null, ExamFaqConversionEntity::getTagName, req.tagName())
        .like(req.keyword() != null, ExamFaqConversionEntity::getStandardQuestion, req.keyword())
        .orderByDesc(ExamFaqConversionEntity::getId);

    long page = req.page() != null ? req.page() : 1;
    long pageSize = req.pageSize() != null ? req.pageSize() : 10;
    Page<ExamFaqConversionEntity> p = faqConversionMapper.selectPage(new Page<>(page, pageSize), wrapper);
    return new PageResponse<>(
        p.getRecords().stream().map(FaqConversionVO::from).toList(),
        p.getCurrent(), p.getSize(), p.getTotal());
  }

  /**
   * 恢复批次：改状态为 running + 重新启动异步 LLM 处理 pending 记录。
   */
  public void resumeBatch(Long tenantId, Long userId, Long batchId) {
    batchService.resumeBatch(tenantId, batchId);

    // 恢复前把上一次 pause/crash 残留在 processing 的记录重置为 pending，否则 loadPendingRecords 会跳过它们
    int resetCount = resetProcessingRecords(tenantId, batchId);

    ExamFaqConversionBatchEntity batch = batchService.getBatchEntity(tenantId, batchId);
    log.info("FAQ 恢复批次启动异步 LLM：batchId={}, resetCount={}", batchId, resetCount);

    String modelCode = batch.getModelCode();
    CompletableFuture.runAsync(() -> {
      processLlmEnhancementAsync(batchId, tenantId, userId, modelCode);
    }, faqConversionExecutor).exceptionally(ex -> {
      log.error("FAQ 恢复批次异步 LLM 未捕获异常：batchId={}, error={}", batchId, ex.getMessage(), ex);
      return null;
    });
  }

  /**
   * 重置批次中 processing 状态记录为 pending（用于 resume，避免记录卡在中间态）。
   */
  private int resetProcessingRecords(Long tenantId, Long batchId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      List<ExamFaqConversionEntity> processing = faqConversionMapper.selectList(
          new LambdaQueryWrapper<ExamFaqConversionEntity>()
              .eq(ExamFaqConversionEntity::getTenantId, tenantId)
              .eq(ExamFaqConversionEntity::getBatchId, batchId)
              .eq(ExamFaqConversionEntity::getConversionStatus, "processing"));
      for (ExamFaqConversionEntity entity : processing) {
        entity.setConversionStatus("pending");
        entity.setErrorMessage(null);
        entity.setUpdatedAt(LocalDateTime.now());
        faqConversionMapper.updateById(entity);
      }
      return processing.size();
    } finally {
      TenantContextHolder.clear();
    }
  }

  /**
   * 重试失败记录：恢复批次 → 重置 failed → pending → 重新启动异步处理。
   *
   * <p>性能优化：批量 UPDATE + 批量计数器调整，将 ~839 次 DB 操作压缩为 3 次，
   * 避免 25 秒前端超时（279 条记录场景下原耗时 ~43 秒）。
   */
  @Transactional
  public void retryFailed(Long tenantId, Long userId, Long batchId) {
    List<ExamFaqConversionEntity> failedRecords = faqConversionMapper.selectList(
        new LambdaQueryWrapper<ExamFaqConversionEntity>()
            .eq(ExamFaqConversionEntity::getTenantId, tenantId)
            .eq(ExamFaqConversionEntity::getBatchId, batchId)
            .eq(ExamFaqConversionEntity::getConversionStatus, "failed"));

    if (failedRecords.isEmpty()) {
      return;
    }

    // 1. 先恢复批次为 running，否则异步启动后会因状态 paused 立即退出
    batchService.ensureRunning(tenantId, batchId);

    // 2. 批量 UPDATE：一条 SQL 将所有 failed 重置为 pending
    faqConversionMapper.update(null, new LambdaUpdateWrapper<ExamFaqConversionEntity>()
        .eq(ExamFaqConversionEntity::getTenantId, tenantId)
        .eq(ExamFaqConversionEntity::getBatchId, batchId)
        .eq(ExamFaqConversionEntity::getConversionStatus, "failed")
        .set(ExamFaqConversionEntity::getConversionStatus, "pending")
        .set(ExamFaqConversionEntity::getErrorMessage, null)
        .set(ExamFaqConversionEntity::getUpdatedAt, LocalDateTime.now()));

    // 3. 计数器一次性调整（failedCount -= count, pendingCount += count）
    int count = failedRecords.size();
    batchService.decrementFailedBy(batchId, count);
    batchService.incrementPendingBy(batchId, count);

    ExamFaqConversionBatchEntity batch = batchService.getBatchEntity(tenantId, batchId);
    log.info("FAQ 重试失败记录启动异步 LLM：batchId={}, count={}", batchId, count);

    // 异步在事务成功提交后由 @TransactionalEventListener(AFTER_COMMIT) 启动
    eventPublisher.publishEvent(new FaqEnhancementStartEvent(batchId, tenantId, userId, batch.getModelCode()));
  }

  /**
   * FAQ 增强启动事件监听器：事务成功提交后启动异步 LLM 处理。
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onFaqEnhancementStart(FaqEnhancementStartEvent event) {
    log.info("FAQ 异步增强启动：batchId={}, tenantId={}, modelCode={}",
        event.batchId(), event.tenantId(), event.modelCode());
    CompletableFuture.runAsync(() -> {
      processLlmEnhancementAsync(event.batchId(), event.tenantId(), event.userId(), event.modelCode());
    }, faqConversionExecutor).exceptionally(ex -> {
      log.error("FAQ 异步增强未捕获异常：batchId={}, error={}", event.batchId(), ex.getMessage(), ex);
      return null;
    });
  }

  /** 查单条转换记录详情（含 JSON 反序列化） */
  @Transactional(readOnly = true)
  public Map<String, Object> getDetail(Long tenantId, Long id) {
    ExamFaqConversionEntity entity = faqConversionMapper.selectById(id);
    if (entity == null || !entity.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "转换记录不存在");
    }
    return Map.of(
        "id", entity.getId(),
        "examId", entity.getExamId() != null ? entity.getExamId() : 0,
        "standardQuestion", entity.getStandardQuestion() != null ? entity.getStandardQuestion() : "",
        "answers", parseJsonArray(entity.getAnswers()),
        "similarQuestions", parseJsonArray(entity.getSimilarQuestions()),
        "negativeQuestions", parseJsonArray(entity.getNegativeQuestions()),
        "tagName", entity.getTagName() != null ? entity.getTagName() : "",
        "conversionStatus", entity.getConversionStatus(),
        "errorMessage", entity.getErrorMessage() != null ? entity.getErrorMessage() : ""
    );
  }

  /**
   * 导出批次下所有 success 状态的 FAQ 为 zip 文件。
   * 按 tag_name 分组，每组一个 JSON 文件，打包为 zip。
   *
   * @return zip 文件字节数组
   */
  @Transactional(readOnly = true)
  public byte[] exportBatch(Long tenantId, Long batchId) {
    List<ExamFaqConversionEntity> successRecords = faqConversionMapper.selectList(
        new LambdaQueryWrapper<ExamFaqConversionEntity>()
            .eq(ExamFaqConversionEntity::getTenantId, tenantId)
            .eq(ExamFaqConversionEntity::getBatchId, batchId)
            .eq(ExamFaqConversionEntity::getConversionStatus, "success"));

    if (successRecords.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该批次没有成功转换的记录可导出");
    }

    // 按 tag_name 分组
    Map<String, List<ExamFaqConversionEntity>> grouped = new LinkedHashMap<>();
    for (ExamFaqConversionEntity record : successRecords) {
      String tag = record.getTagName() != null ? record.getTagName() : "未分类";
      grouped.computeIfAbsent(tag, k -> new ArrayList<>()).add(record);
    }

    // 打包为 zip
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ZipOutputStream zos = new ZipOutputStream(baos)) {

      for (Map.Entry<String, List<ExamFaqConversionEntity>> entry : grouped.entrySet()) {
        String tagFileName = entry.getKey().replaceAll("[\\\\/:*?\"<>|]", "_") + ".json";
        List<Map<String, Object>> faqList = entry.getValue().stream().map(entity -> Map.<String, Object>of(
            "standard_question", entity.getStandardQuestion() != null ? entity.getStandardQuestion() : "",
            "answers", parseJsonArray(entity.getAnswers()),
            "similar_questions", parseJsonArray(entity.getSimilarQuestions()),
            "negative_questions", parseJsonArray(entity.getNegativeQuestions()),
            "tag_name", entity.getTagName() != null ? entity.getTagName() : ""
        )).toList();

        String jsonContent;
        try {
          jsonContent = objectMapper.writeValueAsString(faqList);
        } catch (Exception e) {
          jsonContent = faqList.toString();
        }

        ZipEntry zipEntry = new ZipEntry(tagFileName);
        zos.putNextEntry(zipEntry);
        zos.write(jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
      }

      zos.finish();
      log.info("FAQ 导出完成：batchId={}, successCount={}, tagCount={}", batchId, successRecords.size(), grouped.size());
      return baos.toByteArray();
    } catch (java.io.IOException e) {
      log.error("FAQ 导出 zip 打包失败：batchId={}, error={}", batchId, e.getMessage());
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出文件生成失败");
    }
  }

  // ──────────────────────── private helpers ────────────────────────

  private ExamExamDetailEntity toExamDetail(ExamFaqConversionEntity conv) {
    ExamExamDetailEntity exam = new ExamExamDetailEntity();
    exam.setId(conv.getExamId());
    exam.setType(conv.getSourceType());
    exam.setContent(conv.getSourceContent());
    exam.setAnswer(conv.getSourceAnswer());
    return exam;
  }

  private void checkBatchCompletion(Long batchId, Long tenantId) {
    try {
      ExamFaqConversionBatchEntity batch = batchService.getBatchEntity(tenantId, batchId);
      if (batch != null && batch.getPendingCount() != null && batch.getPendingCount() <= 0) {
        batchService.completeBatch(tenantId, batchId);
      }
    } catch (Exception e) {
      log.warn("检查批次完成状态异常：batchId={}, error={}", batchId, e.getMessage());
    }
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      return "[]";
    }
  }

  private List<String> parseJsonArray(String json) {
    if (json == null || json.isBlank()) return List.of();
    try {
      return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return List.of(json);
    }
  }

  // ──────────────────────── 熔断器 ────────────────────────

  /**
   * 熔断器 — 每个批次一个实例（processLlmEnhancementAsync 的局部变量）。
   * 连续 N 次 429 → isOpen() 返回 true → 调用方暂停批次。
   */
  private static class CircuitBreaker {
    private final int threshold;
    private final AtomicInteger consecutive429 = new AtomicInteger(0);

    CircuitBreaker(int threshold) {
      this.threshold = threshold;
    }

    void onHttp429() {
      consecutive429.incrementAndGet();
    }

    void onSuccess() {
      consecutive429.set(0);
    }

    boolean isOpen() {
      return consecutive429.get() >= threshold;
    }
  }
}
