package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.infrastructure.FaqLlmResponseParser;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FAQ 转换编排服务 — 读题 → 调 LLM → 入库 → 更新进度。
 *
 * 注意：异步处理(>200 题)时，tenantId 由 HTTP 线程捕获后通过参数传递，
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
      ThreadPoolTaskExecutor faqConversionExecutor) {
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

    // 所有批次统一走异步 LLM 增强，HTTP 立即返回 batchId，前端轮询进度
    Long batchId = batch.getId();
    CompletableFuture.runAsync(() -> {
      processLlmEnhancementAsync(batchId, tenantId, userId, req.modelCode(), conversionEntities);
    }, faqConversionExecutor).exceptionally(ex -> {
      log.error("FAQ LLM 异步增强未捕获异常：batchId={}, error={}", batchId, ex.getMessage(), ex);
      return null;
    });

    return FaqBatchVO.from(batch);
  }

  /**
   * LLM 增强（相似问/反例）— 同步执行。
   */
  @Transactional
  public void processLlmEnhancementSync(Long batchId, Long tenantId, Long userId,
                                        String modelCode, List<ExamFaqConversionEntity> entities) {
    ExamFaqModelConfigEntity modelConfig = modelConfigService.findByModelCode(tenantId, modelCode);
    if (modelConfig == null) {
      log.error("模型配置不存在：tenantId={}, modelCode={}", tenantId, modelCode);
      return;
    }
    for (ExamFaqConversionEntity entity : entities) {
      processSingleLlmEnhancement(entity, tenantId, modelConfig, batchId);
    }
    // 检查是否全部完成
    checkBatchCompletion(batchId, tenantId);
  }

  /**
   * LLM 增强 — 异步执行（CompletableFuture）。
   * 注意：此方法在 Worker 线程中执行，不能调 SecurityContext。
   */
  public void processLlmEnhancementAsync(Long batchId, Long tenantId, Long userId,
                                          String modelCode, List<ExamFaqConversionEntity> entities) {
    log.info("FAQ LLM 异步增强开始：batchId={}, tenantId={}, modelCode={}, count={}", batchId, tenantId, modelCode, entities.size());
    ExamFaqModelConfigEntity modelConfig = modelConfigService.findByModelCode(tenantId, modelCode);
    if (modelConfig == null) {
      log.error("模型配置不存在：tenantId={}, modelCode={}", tenantId, modelCode);
      return;
    }

    AtomicInteger processed = new AtomicInteger(0);
    int total = entities.size();

    for (ExamFaqConversionEntity entity : entities) {
      // 检查批次是否被暂停
      if (!batchService.isRunning(tenantId, batchId)) {
        log.info("批次已暂停/取消，停止处理：batchId={}", batchId);
        break;
      }

      processSingleLlmEnhancement(entity, tenantId, modelConfig, batchId);
      processed.incrementAndGet();

      if (processed.get() % 100 == 0) {
        log.info("FAQ LLM 处理进度：batchId={}, {}/{}", batchId, processed.get(), total);
      }
    }

    log.info("FAQ LLM 异步增强完成：batchId={}, processed={}", batchId, processed.get());
    checkBatchCompletion(batchId, tenantId);
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
   * 重试失败记录。
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

    ExamFaqConversionBatchEntity batch = batchService.getBatchEntity(tenantId, batchId);
    ExamFaqModelConfigEntity modelConfig = modelConfigService.findByModelCode(tenantId, batch.getModelCode());
    if (modelConfig == null) {
      log.error("模型配置不存在，无法重试：tenantId={}, modelCode={}", tenantId, batch.getModelCode());
      return;
    }

    for (ExamFaqConversionEntity entity : failedRecords) {
      processSingleLlmEnhancement(entity, tenantId, modelConfig, batchId);
    }

    log.info("FAQ 重试失败记录完成：batchId={}, count={}", batchId, failedRecords.size());
  }

  /**
   * 恢复批次：改状态为 running + 重新启动异步 LLM 处理 pending 记录。
   */
  public void resumeBatch(Long tenantId, Long userId, Long batchId) {
    batchService.resumeBatch(tenantId, batchId);

    // 取出所有 pending 记录，重新启动异步处理
    List<ExamFaqConversionEntity> pendingRecords = faqConversionMapper.selectList(
        new LambdaQueryWrapper<ExamFaqConversionEntity>()
            .eq(ExamFaqConversionEntity::getTenantId, tenantId)
            .eq(ExamFaqConversionEntity::getBatchId, batchId)
            .eq(ExamFaqConversionEntity::getConversionStatus, "pending"));

    if (pendingRecords.isEmpty()) {
      log.info("恢复批次无 pending 记录：batchId={}", batchId);
      return;
    }

    ExamFaqConversionBatchEntity batch = batchService.getBatchEntity(tenantId, batchId);
    log.info("FAQ 恢复批次启动异步 LLM：batchId={}, pendingCount={}", batchId, pendingRecords.size());

    CompletableFuture.runAsync(() -> {
      processLlmEnhancementAsync(batchId, tenantId, userId, batch.getModelCode(), pendingRecords);
    }, faqConversionExecutor).exceptionally(ex -> {
      log.error("FAQ 恢复批次异步 LLM 未捕获异常：batchId={}, error={}", batchId, ex.getMessage(), ex);
      return null;
    });
  }

  /**
   * 查询批次进度（用于前端轮询）。
   */
  @Transactional(readOnly = true)
  public FaqBatchVO getBatchProgress(Long tenantId, Long batchId) {
    return batchService.getById(tenantId, batchId);
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

  // ──────────────────────── private ────────────────────────

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
    // 规则生成 standard_question / answers / tag_name
    String pathForTag = knowledgePath;
    if (pathForTag == null && exam.getKeyword() != null && !exam.getKeyword().isBlank()) {
      // 回退到 keyword 第一个
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

  /**
   * 对单条记录执行 LLM 增强（生成 similar + negative），最多重试 3 次。
   */
  private void processSingleLlmEnhancement(
      ExamFaqConversionEntity entity, Long tenantId,
      ExamFaqModelConfigEntity modelConfig, Long batchId) {

    if ("skipped".equals(entity.getConversionStatus())) {
      return;
    }

    int maxRetries = 3;
    String lastError = null;

    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        // 标记为转换中
        entity.setConversionStatus("processing");
        faqConversionMapper.updateById(entity);

        // 准备 prompt 上下文
        String knowledgePath = pathResolver.resolveLastThreeLevels(entity.getKnowledgeId(), tenantId);
        Map<String, String> promptCtx = faqConverter.buildLlmPromptContext(
            toExamDetail(entity), knowledgePath);

        // 生成 similar_questions
        String similarPrompt = promptBuilder.buildSimilarQuestionsPrompt(promptCtx);
        String similarRaw = llmClient.chatCompletion(
            modelConfig.getBaseUrl(), modelConfig.getApiKey(),
            modelConfig.getModelId(), similarPrompt,
            modelConfig.getMaxTokens(), tenantId);
        List<String> similarQuestions = llmParser.extractJsonArray(similarRaw, "similar_questions");

        // 生成 negative_questions
        String negativePrompt = promptBuilder.buildNegativeQuestionsPrompt(promptCtx);
        String negativeRaw = llmClient.chatCompletion(
            modelConfig.getBaseUrl(), modelConfig.getApiKey(),
            modelConfig.getModelId(), negativePrompt,
            modelConfig.getMaxTokens(), tenantId);
        List<String> negativeQuestions = llmParser.extractJsonArray(negativeRaw, "negative_questions");

        // 序列化为 JSON
        entity.setSimilarQuestions(toJson(similarQuestions));
        entity.setNegativeQuestions(toJson(negativeQuestions));
        entity.setConversionStatus("success");
        entity.setErrorMessage(null);
        entity.setRetryCount(attempt);
        entity.setUpdatedAt(LocalDateTime.now());
        faqConversionMapper.updateById(entity);

        batchService.incrementSuccess(batchId);
        batchService.decrementPending(batchId);
        return; // 成功，不再重试

      } catch (Exception e) {
        lastError = e.getMessage();
        log.warn("FAQ LLM 增强失败（第{}次）：conversionId={}, error={}",
            attempt + 1, entity.getId(), lastError);
        // 重试之间等 300ms，避免连续快速重试触发限流
        if (attempt < maxRetries) {
          try {
            Thread.sleep(300);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }

    // 全部重试失败 → error 级别记录，因为这是最终失败
    entity.setConversionStatus("failed");
    entity.setErrorMessage(lastError != null ? lastError.substring(0, Math.min(lastError.length(), 1024)) : "未知错误");
    entity.setRetryCount(maxRetries);
    entity.setUpdatedAt(LocalDateTime.now());
    faqConversionMapper.updateById(entity);
    log.error("FAQ LLM 增强最终失败（已重试{}次）：conversionId={}, examId={}, error={}",
        maxRetries, entity.getId(), entity.getExamId(), lastError);

    batchService.incrementFailed(batchId);
    batchService.decrementPending(batchId);
  }

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
}
