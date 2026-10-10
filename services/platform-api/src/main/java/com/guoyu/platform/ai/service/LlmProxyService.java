package com.guoyu.platform.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.ai.dto.LlmChatCompletionReq;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * LLM 代理编排服务 — 查配置 → 调 LLM → 返回原始响应。
 *
 * <p>安全约束：
 * <ul>
 *   <li>tenantId 必须取自 {@link SecurityContext#currentUser()}（token 中的真实租户），
 *       禁止使用 TenantContextHolder（来自 X-Tenant-Id header，可伪造）</li>
 *   <li>模型配置按租户隔离 + 消耗 LLM 配额，伪造 X-Tenant-Id 会导致双条件冲突查不到配置（fail-closed）</li>
 * </ul>
 */
@Service
@Slf4j
public class LlmProxyService {

  private final FaqModelConfigApplicationService modelConfigService;
  private final FaqLlmClient llmClient;

  public LlmProxyService(FaqModelConfigApplicationService modelConfigService,
                         FaqLlmClient llmClient) {
    this.modelConfigService = modelConfigService;
    this.llmClient = llmClient;
  }

  /**
   * 同步调用 LLM 并返回完整原始响应。
   *
   * @return LLM 原始响应 JsonNode（含 choices / usage / model）
   */
  public JsonNode chatCompletion(LlmChatCompletionReq req) {
    // ⚠️ 必须用 token 中的真实租户，不能用 header 的 X-Tenant-Id（可伪造）
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    // 查模型配置
    ExamFaqModelConfigEntity modelConfig = modelConfigService.findByModelCode(tenantId, req.modelCode());
    if (modelConfig == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在或未启用");
    }

    // 校验 messages 为非空数组
    JsonNode messages = req.messages();
    if (messages == null || !messages.isArray() || messages.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "messages 必须为非空数组");
    }

    // 合并参数：请求传了用请求值，否则用配置值
    int maxTokens = req.maxTokens() != null ? req.maxTokens() : modelConfig.getMaxTokens();
    var temperature = req.temperature() != null ? req.temperature() : modelConfig.getTemperature();
    String thinking = req.thinking() != null ? req.thinking() : modelConfig.getThinking();
    // reasoning_effort：请求传了用请求值，否则用配置值（DB 默认 high；仅 DeepSeek+thinking=enabled 发送）
    String reasoningEffort = req.reasoningEffort() != null
        ? req.reasoningEffort() : modelConfig.getReasoningEffort();
    boolean jsonMode = req.responseFormatJson() != null && req.responseFormatJson();

    log.info("LLM 代理调用：userId={}, tenantId={}, modelCode={}, modelId={}, messages={}",
        userId, tenantId, req.modelCode(), modelConfig.getModelId(), messages.size());

    long startMs = System.currentTimeMillis();

    // 调 LLM（代理模式：messages 透传，仅 HTTP 级分类）
    JsonNode response = llmClient.chatCompletionMessages(
        modelConfig.getBaseUrl(), modelConfig.getApiKey(),
        modelConfig.getModelId(), messages, maxTokens,
        temperature, thinking, reasoningEffort, jsonMode, tenantId);

    long elapsed = System.currentTimeMillis() - startMs;

    // 日志留痕（不做限流，配额消耗靠日志追溯）
    int totalTokens = response.path("usage").path("total_tokens").asInt(0);
    log.info("LLM 代理完成：userId={}, tenantId={}, modelCode={}, totalTokens={}, elapsed={}ms",
        userId, tenantId, req.modelCode(), totalTokens, elapsed);

    return response;
  }
}
