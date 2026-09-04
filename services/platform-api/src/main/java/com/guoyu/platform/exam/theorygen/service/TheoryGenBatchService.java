package com.guoyu.platform.exam.theorygen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.theorygen.dto.BlueprintParseResult.NodeRow;
import com.guoyu.platform.exam.theorygen.dto.TheoryGenBatchVO;
import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenBatchEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryBlueprintNodeMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenBatchMapper;
import com.guoyu.platform.exam.theorygen.mapper.TheoryGenDraftMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 理论试题生成批次：提交（秒回）→ 异步分块（1 考评点 = 1 次 LLM 调用）→ 草稿落库。
 * 线程边界：submit/pause/resume 走 HTTP 线程；processBatch 全链路跑 theory-gen- Worker，
 * tenantId/userId 参数传递，禁止调 SecurityContext（陷阱 #2、踩坑 #15/#17）。
 */
@Service
@Slf4j
public class TheoryGenBatchService {

  private final TheoryBlueprintMapper blueprintMapper;
  private final TheoryBlueprintNodeMapper nodeMapper;
  private final TheoryGenBatchMapper batchMapper;
  private final TheoryGenDraftMapper draftMapper;
  private final ExamFaqModelConfigMapper modelConfigMapper;
  private final TheoryGenAiService aiService;
  private final TheoryQuestionValidator validator;
  private final ObjectMapper objectMapper;
  private final ThreadPoolTaskExecutor theoryGenExecutor;

  public TheoryGenBatchService(TheoryBlueprintMapper blueprintMapper,
      TheoryBlueprintNodeMapper nodeMapper, TheoryGenBatchMapper batchMapper,
      TheoryGenDraftMapper draftMapper, ExamFaqModelConfigMapper modelConfigMapper,
      TheoryGenAiService aiService, TheoryQuestionValidator validator,
      ObjectMapper objectMapper,
      @Qualifier("theoryGenExecutor") ThreadPoolTaskExecutor theoryGenExecutor) {
    this.blueprintMapper = blueprintMapper;
    this.nodeMapper = nodeMapper;
    this.batchMapper = batchMapper;
    this.draftMapper = draftMapper;
    this.modelConfigMapper = modelConfigMapper;
    this.aiService = aiService;
    this.validator = validator;
    this.objectMapper = objectMapper;
    this.theoryGenExecutor = theoryGenExecutor;
  }

  /** 提交生成批次（秒回 batchId；实际生成在 Worker 线程） */
  @Transactional
  public TheoryGenBatchVO submit(Long tenantId, Long userId, Long blueprintId, Long modelConfigId,
      Map<String, Integer> quota) {
    TheoryBlueprintEntity bp = blueprintMapper.selectById(blueprintId);
    if (bp == null || !bp.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "细目表批次不存在");
    }
    // 已有进行中的批次则拒绝重复提交（同蓝图串行）
    Long running = batchMapper.selectCount(new LambdaQueryWrapper<TheoryGenBatchEntity>()
        .eq(TheoryGenBatchEntity::getTenantId, tenantId)
        .eq(TheoryGenBatchEntity::getBlueprintId, blueprintId)
        .in(TheoryGenBatchEntity::getStatus,
            TheoryGenBatchEntity.STATUS_PENDING, TheoryGenBatchEntity.STATUS_RUNNING));
    if (running != null && running > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该细目表已有进行中的生成批次");
    }
    ExamFaqModelConfigEntity cfg = modelConfigMapper.selectById(modelConfigId);
    if (cfg == null || !"enabled".equals(cfg.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在或未启用");
    }
    if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()
        || "PENDING_USER_INPUT".equals(cfg.getApiKey())) {
      throw new BusinessException(ErrorCode.CONFLICT, "模型 api_key 未配置，请先在模型配置中回填");
    }
    // 题量配额：不传用默认（指引下限）；低于下限拒绝
    Map<String, Integer> effectiveQuota = quota == null || quota.isEmpty()
        ? TheoryGenPromptBuilder.defaultQuota(bp.getLevel()) : quota;
    Map<String, Integer> minQuota = TheoryGenPromptBuilder.defaultQuota(bp.getLevel());
    for (Map.Entry<String, Integer> e : minQuota.entrySet()) {
      int actual = effectiveQuota.getOrDefault(e.getKey(), 0);
      if (actual < e.getValue()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "题型 " + e.getKey() + " 题量 " + actual + " 低于指引下限 " + e.getValue());
      }
    }

    List<TheoryBlueprintNodeEntity> points = listPoints(tenantId, blueprintId);
    if (points.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "细目表无考评点，无法生成");
    }

    TheoryGenBatchEntity batch = new TheoryGenBatchEntity();
    batch.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
    batch.setTenantId(tenantId);
    batch.setBlueprintId(blueprintId);
    batch.setModelConfigId(cfg.getId());
    batch.setModelId(cfg.getModelId());
    batch.setModelCode(cfg.getModelCode());
    batch.setThinking(cfg.getThinking());
    batch.setReasoningEffort(cfg.getReasoningEffort());
    batch.setQuotaConfig(writeJson(effectiveQuota));
    batch.setStatus(TheoryGenBatchEntity.STATUS_PENDING);
    batch.setTotalPoints(points.size());
    batch.setDonePoints(0);
    batch.setFailedPoints(0);
    batch.setTotalQuestions(0);
    batch.setTokenUsage(0L);
    batch.setPromptDigest(abbreviate(TheoryGenPromptBuilder.buildSystemPrompt(bp.getLevel(), effectiveQuota), 200));
    batch.setCreatedBy(userId);
    batch.setUpdatedBy(userId);
    batch.setCreatedAt(LocalDateTime.now());
    batch.setUpdatedAt(LocalDateTime.now());
    batch.setDeleted(0);
    batchMapper.insert(batch);

    bp.setStatus("generating");
    bp.setUpdatedAt(LocalDateTime.now());
    blueprintMapper.updateById(bp);

    // 提交异步编排（AbortPolicy：池满抛 RejectedExecutionException → 503 由全局异常兜底）
    try {
      theoryGenExecutor.submit(() -> processBatch(batch.getId(), tenantId, userId, bp.getLevel()));
    } catch (RejectedExecutionException e) {
      batch.setStatus(TheoryGenBatchEntity.STATUS_FAILED);
      batch.setFailReason("服务繁忙，请稍后重试");
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return TheoryGenBatchVO.from(batch);
  }

  /**
   * Worker 线程：批次编排。每考评点一次 LLM 调用；暂停只挡新块（已提交块跑完，踩坑 #18）。
   * 全链路不调 SecurityContext；Worker 自行 set/clear TenantContextHolder。
   */
  public void processBatch(Long batchId, Long tenantId, Long userId, int level) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      TheoryGenBatchEntity batch = batchMapper.selectById(batchId);
      if (batch == null) {
        log.error("理论生成批次不存在：batchId={}", batchId);
        return;
      }
      batch.setStatus(TheoryGenBatchEntity.STATUS_RUNNING);
      batch.setStartedAt(LocalDateTime.now());
      batch.setUpdatedAt(LocalDateTime.now());
      batchMapper.updateById(batch);

      ExamFaqModelConfigEntity cfg = modelConfigMapper.selectById(batch.getModelConfigId());
      if (cfg == null) {
        failBatch(batch, "模型配置不存在");
        return;
      }
      Map<String, Integer> quota = readJson(batch.getQuotaConfig());
      String systemPrompt = TheoryGenPromptBuilder.buildSystemPrompt(level, quota);
      List<TheoryBlueprintNodeEntity> points = listPoints(tenantId, batch.getBlueprintId());
      // 批次内不变数据一次性加载（踩坑 #18）：节点树已随 points 一次查出

      int consecutiveFailures = 0;
      for (TheoryBlueprintNodeEntity point : points) {
        // 暂停检查：每块开始前重读状态
        TheoryGenBatchEntity current = batchMapper.selectById(batchId);
        if (current == null || !TheoryGenBatchEntity.STATUS_RUNNING.equals(current.getStatus())) {
          log.info("批次已暂停/终止，停止提交新块：batchId={}", batchId);
          return;
        }
        try {
          long t0 = System.currentTimeMillis();
          String raw = aiService.generateForPoint(tenantId, cfg.getBaseUrl(), cfg.getApiKey(),
              cfg.getModelId(), batch.getThinking(), batch.getReasoningEffort(),
              cfg.getMaxTokens() == null ? 4096 : cfg.getMaxTokens(),
              systemPrompt, TheoryGenPromptBuilder.buildUserPrompt(point, quota));
          List<TheoryQuestionItem> questions = aiService.parseQuestions(raw);
          // 校验每题；任一题不合法 → 整块失败（零信任，不猜不兜底）
          for (TheoryQuestionItem q : questions) {
            List<String> errors = validator.validate(q);
            if (!errors.isEmpty()) {
              throw new BusinessException(ErrorCode.BAD_GATEWAY,
                  "考评点 " + point.getFullCode() + " 产出不合法: " + String.join("；", errors));
            }
          }
          List<String> dup = validator.checkDuplicates(questions);
          if (!dup.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_GATEWAY,
                "考评点 " + point.getFullCode() + " 题干重复: " + String.join("；", dup));
          }
          saveDrafts(batch, point, questions, (int) (System.currentTimeMillis() - t0));
          batchMapper.incrementDone(batchId, questions.size(), 0);
          consecutiveFailures = 0;
        } catch (Exception e) {
          log.warn("考评点生成失败：batchId={}, point={}, err={}", batchId, point.getFullCode(), e.getMessage());
          batchMapper.incrementFailed(batchId);
          consecutiveFailures++;
          if (consecutiveFailures >= 5) {
            failBatch(batch, "连续 5 个考评点失败（" + abbreviate(e.getMessage(), 300) + "），批次终止，可重试失败块");
            return;
          }
        }
      }
      // 收尾
      TheoryGenBatchEntity finalState = batchMapper.selectById(batchId);
      if (finalState != null && TheoryGenBatchEntity.STATUS_RUNNING.equals(finalState.getStatus())) {
        finalState.setStatus(TheoryGenBatchEntity.STATUS_DONE);
        finalState.setFinishedAt(LocalDateTime.now());
        finalState.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(finalState);
        TheoryBlueprintEntity bp = blueprintMapper.selectById(finalState.getBlueprintId());
        if (bp != null) {
          bp.setStatus("done");
          bp.setUpdatedAt(LocalDateTime.now());
          blueprintMapper.updateById(bp);
        }
      }
      log.info("理论生成批次完成：batchId={}", batchId);
    } catch (Exception e) {
      log.error("理论生成批次异常终止：batchId={}", batchId, e);
      TheoryGenBatchEntity batch = batchMapper.selectById(batchId);
      if (batch != null) {
        failBatch(batch, abbreviate(e.getMessage(), 300));
      }
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** 草稿落库（Worker 线程；tenantId 已在 ThreadLocal） */
  private void saveDrafts(TheoryGenBatchEntity batch, TheoryBlueprintNodeEntity point,
      List<TheoryQuestionItem> questions, int durationMs) {
    Long userId = batch.getCreatedBy();
    for (TheoryQuestionItem q : questions) {
      TheoryGenDraftEntity d = new TheoryGenDraftEntity();
      d.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
      d.setTenantId(batch.getTenantId());
      d.setBatchId(batch.getId());
      d.setBlueprintId(batch.getBlueprintId());
      d.setNodeId(point.getId());
      d.setQuestionType(q.getQuestionType());
      d.setTypeCode(TheoryGenPromptBuilder.typeCodeOf(q.getQuestionType()));
      d.setDifficulty(q.getDifficulty() == null ? 3 : q.getDifficulty());
      d.setConsistency(5);
      d.setContent(q.getContent());
      d.setOptions(serializeOptions(q));
      d.setAnswer(q.getAnswer() == null ? "" : normalizeAnswer(q));
      d.setAnalysis(q.getAnalysis());
      d.setReferenceAnswer(q.getReferenceAnswer());
      d.setScorePoints(q.getScorePoints());
      d.setScoringStandard(q.getScoringStandard());
      d.setScore(q.getScore());
      d.setAiModel(batch.getModelId());
      d.setAiThinking(batch.getThinking());
      d.setGenDurationMs(durationMs);
      d.setReviewStatus(TheoryGenDraftEntity.REVIEW_DRAFT);
      d.setPublishStatus(0);
      d.setCreatedAt(LocalDateTime.now());
      d.setUpdatedAt(LocalDateTime.now());
      d.setCreatedBy(userId);
      d.setUpdatedBy(userId);
      d.setDeleted(0);
      draftMapper.insert(d);
    }
  }

  /** 判断题答案归一：true/false → √/× */
  private String normalizeAnswer(TheoryQuestionItem q) {
    String a = q.getAnswer().trim();
    if ("judge".equals(q.getQuestionType())) {
      if (a.equalsIgnoreCase("true")) {
        return "√";
      }
      if (a.equalsIgnoreCase("false")) {
        return "×";
      }
    }
    return a.toUpperCase();
  }

  private String serializeOptions(TheoryQuestionItem q) {
    if (q.getOptionList() == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(q.getOptionList());
    } catch (Exception e) {
      return null;
    }
  }

  private void failBatch(TheoryGenBatchEntity batch, String reason) {
    batch.setStatus(TheoryGenBatchEntity.STATUS_FAILED);
    batch.setFailReason(reason);
    batch.setFinishedAt(LocalDateTime.now());
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
  }

  /** 暂停（HTTP 线程） */
  @Transactional
  public TheoryGenBatchVO pause(Long tenantId, Long batchId) {
    TheoryGenBatchEntity batch = getOwned(tenantId, batchId);
    if (!TheoryGenBatchEntity.STATUS_RUNNING.equals(batch.getStatus())
        && !TheoryGenBatchEntity.STATUS_PENDING.equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次不在运行中，无法暂停");
    }
    batch.setStatus(TheoryGenBatchEntity.STATUS_PAUSED);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    return TheoryGenBatchVO.from(batch);
  }

  /** 继续（HTTP 线程 → 重新提交编排） */
  @Transactional
  public TheoryGenBatchVO resume(Long tenantId, Long userId, Long batchId) {
    TheoryGenBatchEntity batch = getOwned(tenantId, batchId);
    if (!TheoryGenBatchEntity.STATUS_PAUSED.equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次不在暂停状态，无法继续");
    }
    batch.setStatus(TheoryGenBatchEntity.STATUS_PENDING);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    TheoryBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    int level = bp != null && bp.getLevel() != null ? bp.getLevel() : 3;
    try {
      theoryGenExecutor.submit(() -> processBatch(batchId, tenantId, userId, level));
    } catch (RejectedExecutionException e) {
      batch.setStatus(TheoryGenBatchEntity.STATUS_PAUSED);
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return TheoryGenBatchVO.from(batch);
  }

  /** 重试失败块：failed_points 归零并将状态置回 pending（已完成草稿保留） */
  @Transactional
  public TheoryGenBatchVO retryFailed(Long tenantId, Long userId, Long batchId) {
    TheoryGenBatchEntity batch = getOwned(tenantId, batchId);
    if (!TheoryGenBatchEntity.STATUS_FAILED.equals(batch.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次不在失败状态，无法重试");
    }
    batch.setStatus(TheoryGenBatchEntity.STATUS_PENDING);
    batch.setFailReason(null);
    batch.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(batch);
    TheoryBlueprintEntity bp = blueprintMapper.selectById(batch.getBlueprintId());
    int level = bp != null && bp.getLevel() != null ? bp.getLevel() : 3;
    try {
      theoryGenExecutor.submit(() -> processBatch(batchId, tenantId, userId, level));
    } catch (RejectedExecutionException e) {
      batch.setStatus(TheoryGenBatchEntity.STATUS_FAILED);
      batchMapper.updateById(batch);
      throw new BusinessException(ErrorCode.CONFLICT, "服务繁忙，请稍后重试");
    }
    return TheoryGenBatchVO.from(batch);
  }

  /** 批次分页 */
  @Transactional(readOnly = true)
  public PageResponse<TheoryGenBatchVO> page(Long tenantId, Long blueprintId, long page, long pageSize) {
    LambdaQueryWrapper<TheoryGenBatchEntity> w = new LambdaQueryWrapper<TheoryGenBatchEntity>()
        .eq(TheoryGenBatchEntity::getTenantId, tenantId)
        .eq(blueprintId != null, TheoryGenBatchEntity::getBlueprintId, blueprintId)
        .orderByDesc(TheoryGenBatchEntity::getId);
    Page<TheoryGenBatchEntity> p = batchMapper.selectPage(new Page<>(page, pageSize), w);
    return new PageResponse<>(
        p.getRecords().stream().map(TheoryGenBatchVO::from).toList(),
        p.getCurrent(), p.getSize(), p.getTotal());
  }

  /** 批次详情/进度（前端轮询） */
  @Transactional(readOnly = true)
  public TheoryGenBatchVO getProgress(Long tenantId, Long batchId) {
    return TheoryGenBatchVO.from(getOwned(tenantId, batchId));
  }

  /** 启用中的模型配置（前端下拉推荐值） */
  @Transactional(readOnly = true)
  public List<com.guoyu.platform.exam.theorygen.dto.TheoryModelConfigVO> listEnabledModelConfigs(Long tenantId) {
    List<ExamFaqModelConfigEntity> list = modelConfigMapper.selectList(
        new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
            .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
            .eq(ExamFaqModelConfigEntity::getStatus, "enabled")
            .orderByDesc(ExamFaqModelConfigEntity::getId));
    return list == null ? List.of()
        : list.stream().map(com.guoyu.platform.exam.theorygen.dto.TheoryModelConfigVO::from).toList();
  }

  private TheoryGenBatchEntity getOwned(Long tenantId, Long batchId) {
    TheoryGenBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "生成批次不存在");
    }
    return batch;
  }

  private List<TheoryBlueprintNodeEntity> listPoints(Long tenantId, Long blueprintId) {
    return nodeMapper.selectList(new LambdaQueryWrapper<TheoryBlueprintNodeEntity>()
        .eq(TheoryBlueprintNodeEntity::getTenantId, tenantId)
        .eq(TheoryBlueprintNodeEntity::getBlueprintId, blueprintId)
        .eq(TheoryBlueprintNodeEntity::getNodeLevel, 4)
        .orderByAsc(TheoryBlueprintNodeEntity::getSortOrder));
  }

  private String writeJson(Map<String, Integer> quota) {
    try {
      return objectMapper.writeValueAsString(quota);
    } catch (Exception e) {
      return "{}";
    }
  }

  private Map<String, Integer> readJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() { });
    } catch (Exception e) {
      return new HashMap<>();
    }
  }

  private String abbreviate(String s, int max) {
    if (s == null) {
      return "";
    }
    // 预留 1 个字符给省略号，确保结果不超过 max（prompt_digest VARCHAR(200) 曾因 200+1 超长）
    return s.length() <= max ? s : s.substring(0, max - 1) + "…";
  }
}
