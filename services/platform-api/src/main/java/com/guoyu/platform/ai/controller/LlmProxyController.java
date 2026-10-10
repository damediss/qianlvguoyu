package com.guoyu.platform.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.ai.dto.LlmChatCompletionReq;
import com.guoyu.platform.ai.service.LlmProxyService;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LLM 代理 REST API — iframe 内 Python 应用通过 postMessage 获取 token 后直连。
 *
 * <p>鉴权：复用现有 Sa-Token（X-Tenant-Id + Authorization header），
 * 权限码 ai:llm:proxy 控制访问。
 */
@RestController
@RequestMapping("/ai/llm")
@Slf4j
public class LlmProxyController {

  private final LlmProxyService proxyService;

  public LlmProxyController(LlmProxyService proxyService) {
    this.proxyService = proxyService;
  }

  /** OpenAI 兼容 Chat Completion 代理 — messages 透传，返回完整原始响应 */
  @PostMapping("/chat-completion")
  @PreAuthorize("hasAuthority('ai:llm:proxy')")
  @AuditLog(module = "ai", action = "llm:proxy:call", targetType = "llm_api")
  public ApiResponse<JsonNode> chatCompletion(@Valid @RequestBody LlmChatCompletionReq req) {
    return ApiResponse.ok(proxyService.chatCompletion(req));
  }
}
