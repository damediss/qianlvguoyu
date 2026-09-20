package com.guoyu.platform.courseware.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.ai.dto.LlmChatCompletionResult;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.web.TraceIdHolder;
import com.guoyu.platform.courseware.domain.CoursewareJson;
import com.guoyu.platform.courseware.entity.CwLlmLogEntity;
import com.guoyu.platform.courseware.mapper.CwLlmLogMapper;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 课件生成 LLM 调用收敛层(LLM 规范 #1):业务代码只依赖本服务;
 * 模型/密钥走 exam_faq_model_config 配置(#2);输出 JSON 由调用方校验(#5)。
 * 全程租户参数传递,不调 SecurityContext(陷阱 #2)。
 */
@Slf4j
@Service
public class CoursewareAiService {

  private final FaqLlmClient llmClient;
  private final ExamFaqModelConfigMapper modelConfigMapper;
  private final CwLlmLogMapper llmLogMapper;
  private final String modelCode;
  private final int responseTimeoutSeconds;

  public CoursewareAiService(
      FaqLlmClient llmClient,
      ExamFaqModelConfigMapper modelConfigMapper,
      CwLlmLogMapper llmLogMapper,
      @Value("${platform.courseware.model-code:courseware-gen-glm}") String modelCode,
      @Value("${platform.courseware.llm-timeout-seconds:120}") int responseTimeoutSeconds) {
    this.llmClient = llmClient;
    this.modelConfigMapper = modelConfigMapper;
    this.llmLogMapper = llmLogMapper;
    this.modelCode = modelCode;
    this.responseTimeoutSeconds = responseTimeoutSeconds;
  }

  /** 消化层调用 */
  public String callDigest(Long tenantId, Long projectId, Long operatorId,
                           String systemPrompt, String userPrompt) {
    return call(CwLlmLogEntity.STAGE_DIGEST, tenantId, projectId, null, operatorId,
        systemPrompt, userPrompt);
  }

  /** 大纲生成调用 */
  public String callOutline(Long tenantId, Long projectId, Long operatorId,
                            String systemPrompt, String userPrompt) {
    return call(CwLlmLogEntity.STAGE_OUTLINE, tenantId, projectId, null, operatorId,
        systemPrompt, userPrompt);
  }

  /** 单页生成调用 */
  public String callSlide(Long tenantId, Long projectId, Long slideId, Long operatorId,
                          String systemPrompt, String userPrompt) {
    return call(CwLlmLogEntity.STAGE_SLIDE, tenantId, projectId, slideId, operatorId,
        systemPrompt, userPrompt);
  }

  /** 统一调用:配置加载 → GLM 闸口(FaqLlmClient 内) → 审计落库。异常向上抛由调用方分类处理 */
  private String call(String stage, Long tenantId, Long projectId, Long slideId, Long operatorId,
                      String systemPrompt, String userPrompt) {
    ExamFaqModelConfigEntity cfg = loadConfig(tenantId);
    long start = System.currentTimeMillis();
    try {
      LlmChatCompletionResult result = llmClient.chatCompletionDetailed(
          cfg.getBaseUrl(), cfg.getApiKey(), cfg.getModelId(),
          systemPrompt, userPrompt,
          cfg.getMaxTokens() != null ? cfg.getMaxTokens() : 8192,
          cfg.getThinking(), cfg.getReasoningEffort(),
          true, tenantId, responseTimeoutSeconds);
      audit(stage, tenantId, projectId, slideId, operatorId, cfg, userPrompt, result, 1, null,
          System.currentTimeMillis() - start);
      return result.content();
    } catch (RuntimeException e) {
      audit(stage, tenantId, projectId, slideId, operatorId, cfg, userPrompt, null, 0,
          abbreviate(e.getMessage()), System.currentTimeMillis() - start);
      throw e;
    }
  }

  /** 模型配置加载(Worker 线程安全:显式 tenantId,不调 SecurityContext) */
  ExamFaqModelConfigEntity loadConfig(Long tenantId) {
    ExamFaqModelConfigEntity cfg = modelConfigMapper.selectOne(
        new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
            .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
            .eq(ExamFaqModelConfigEntity::getModelCode, modelCode)
            .eq(ExamFaqModelConfigEntity::getStatus, "enabled")
            .orderByDesc(ExamFaqModelConfigEntity::getId)
            .last("LIMIT 1"));
    if (cfg == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "课件生成模型未配置,请联系管理员");
    }
    if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()
        || "PENDING_USER_INPUT".equals(cfg.getApiKey())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "课件生成模型未配置 API Key,请联系管理员");
    }
    return cfg;
  }

  private void audit(String stage, Long tenantId, Long projectId, Long slideId, Long operatorId,
                     ExamFaqModelConfigEntity cfg, String userPrompt,
                     LlmChatCompletionResult result, int success, String error, long durationMs) {
    try {
      CwLlmLogEntity entry = new CwLlmLogEntity();
      entry.setId(IdWorker.getId());
      entry.setTenantId(tenantId);
      entry.setProjectId(projectId);
      entry.setSlideId(slideId);
      entry.setStage(stage);
      entry.setModelCode(cfg.getModelCode());
      entry.setModelId(cfg.getModelId());
      entry.setPromptDigest(CoursewareJson.digest(userPrompt));
      if (result != null) {
        entry.setPromptTokens((int) result.promptTokens());
        entry.setCompletionTokens((int) result.completionTokens());
        entry.setTotalTokens((int) result.totalTokens());
        entry.setCachedTokens((int) result.cachedTokens());
      }
      entry.setDurationMs(durationMs);
      entry.setSuccess(success);
      entry.setError(error);
      entry.setTraceId(TraceIdHolder.getTraceId());
      entry.setCreatedBy(operatorId);
      entry.setUpdatedBy(operatorId);
      llmLogMapper.insert(entry);
    } catch (Exception e) {
      // 审计失败不阻断主流程,仅告警
      log.warn("课件 LLM 审计落库失败: stage={}, projectId={}, error={}", stage, projectId,
          e.getMessage());
    }
  }

  private String abbreviate(String message) {
    if (message == null) {
      return null;
    }
    return message.length() <= 480 ? message : message.substring(0, 480);
  }
}
