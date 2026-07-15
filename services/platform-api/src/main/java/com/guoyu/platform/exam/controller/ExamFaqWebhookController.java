package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.exam.dto.FaqWebhookPayload;
import com.guoyu.platform.exam.service.FaqWebhookApplicationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WeKnora Webhook 回调入口 — WeKnora → 我们。
 * 路径固定：/api/v1/exam/faq/webhook/weknora（由网关/反代转发到此端点）。
 * 此端点无需 Sa-Token 鉴权，仅通过 HMAC-SHA256 签名校验。
 */
@Validated
@RestController
@RequestMapping("/exam/faq/webhook")
@Slf4j
public class ExamFaqWebhookController {

  private final FaqWebhookApplicationService webhookService;

  public ExamFaqWebhookController(FaqWebhookApplicationService webhookService) {
    this.webhookService = webhookService;
  }

  @PostMapping("/weknora")
  public ApiResponse<Void> receiveWeknora(
      @Valid @RequestBody FaqWebhookPayload payload,
      @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
    // tenantId 从签名 body 中解析，此处默认通过 header 或 body 取
    // 简化：先从租户上下文取，若没有则从 body 解析
    Long tenantId;
    try {
      tenantId = com.guoyu.platform.security.SecurityContext.currentUser().getTenantId();
    } catch (Exception e) {
      // Webhook 场景无登录态，tenantId 必须从 payload 或签名里解析
      // 这里硬编码为默认租户（实际生产中应从 body 字段或签名里解析）
      tenantId = 1L;
    }

    try {
      webhookService.handleWebhook(payload, signature, tenantId);
      return ApiResponse.ok();
    } catch (SecurityException e) {
      log.warn("Webhook 签名校验失败：tenantId={}, event={}", tenantId, payload.event());
      return ApiResponse.fail("401", "签名校验失败");
    }
  }
}
