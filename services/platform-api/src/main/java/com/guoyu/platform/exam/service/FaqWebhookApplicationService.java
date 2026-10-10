package com.guoyu.platform.exam.service;

import com.guoyu.platform.exam.dto.FaqWebhookPayload;
import com.guoyu.platform.exam.entity.ExamFaqWeknoraConfigEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** WeKnora Webhook 处理 — 签名校验 + 事件分发 */
@Service
@Slf4j
public class FaqWebhookApplicationService {

  private final FaqWeknoraConfigApplicationService configService;

  public FaqWebhookApplicationService(FaqWeknoraConfigApplicationService configService) {
    this.configService = configService;
  }

  /**
   * 校验签名并处理 webhook 事件。
   *
   * @param payload   请求体
   * @param signature X-Webhook-Signature 头（sha256=xxx）
   * @param tenantId  租户 ID（从签名 body 解析或从上下文获取）
   * @return true=处理成功，false=忽略（事件未启用）
   * @throws SecurityException 签名不通过
   */
  public boolean handleWebhook(FaqWebhookPayload payload, String signature, Long tenantId) {
    // 加载租户配置
    ExamFaqWeknoraConfigEntity config = configService.getConfigByTenantId(tenantId);
    if (config == null || config.getWebhookEnabled() == null || config.getWebhookEnabled() != 1) {
      log.warn("Webhook 未启用或配置不存在：tenantId={}", tenantId);
      return false;
    }

    // 校验签名
    if (!verifySignature(payload, signature, config.getWebhookSecret())) {
      log.warn("Webhook 签名校验失败：tenantId={}, event={}", tenantId, payload.event());
      throw new SecurityException("Webhook signature verification failed");
    }

    // 检查事件是否启用
    if (!isEventEnabled(config, payload.event())) {
      log.debug("Webhook 事件未启用：tenantId={}, event={}", tenantId, payload.event());
      return false;
    }

    // 按事件类型分发
    switch (payload.event()) {
      case "message_sent" -> handleMessageSent(payload, tenantId);
      case "message_received" -> handleMessageReceived(payload, tenantId);
      default -> log.warn("未知 Webhook 事件：tenantId={}, event={}", tenantId, payload.event());
    }
    return true;
  }

  private void handleMessageSent(FaqWebhookPayload payload, Long tenantId) {
    // 记录访客问题日志（可用于后续分析未匹配问题）
    log.info("WeKnora 访客消息：tenantId={}, conversationId={}, messageId={}, content={}",
        tenantId, payload.conversation_id(), payload.message_id(),
        truncate(payload.content(), 200));
  }

  private void handleMessageReceived(FaqWebhookPayload payload, Long tenantId) {
    // 记录 AI 回复日志（暂不做自动质量评估）
    log.info("WeKnora AI 回复：tenantId={}, conversationId={}, messageId={}, reply={}",
        tenantId, payload.conversation_id(), payload.message_id(),
        truncate(payload.reply_content(), 200));
  }

  private boolean verifySignature(FaqWebhookPayload payload, String signature, String secret) {
    if (secret == null || secret.isBlank()) {
      log.warn("Webhook secret 未配置");
      return false;
    }
    if (signature == null || signature.isBlank()) {
      return false;
    }

    try {
      // 拼接 payload 各字段为 canonical string
      String rawBody = payload.event() + "|" + payload.message_id() + "|"
          + payload.conversation_id() + "|" + payload.content() + "|"
          + payload.reply_content() + "|" + payload.timestamp();

      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
      String expected = "sha256=" + HexFormat.of().formatHex(hash);

      return MessageDigest.isEqual(
          expected.getBytes(StandardCharsets.UTF_8),
          signature.getBytes(StandardCharsets.UTF_8)
      );
    } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
      log.error("Webhook 签名计算异常：{}", e.getMessage());
      return false;
    }
  }

  private boolean isEventEnabled(ExamFaqWeknoraConfigEntity config, String event) {
    String events = config.getEnabledEvents();
    if (events == null || events.isBlank()) return false;
    // 简单 JSON 数组包含检查
    return events.contains("\"" + event + "\"");
  }

  private String truncate(String s, int maxLen) {
    if (s == null) return null;
    return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
  }
}
