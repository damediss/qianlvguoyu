package com.guoyu.platform.integration.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Verifies the compact base64url JSON + HMAC format emitted by mcp-server. */
@Component
@Slf4j
public class BotActorContextCodec {

  public static final String CONTEXT_HEADER = "X-Bot-Actor-Context";
  public static final String SIGNATURE_HEADER = "X-Bot-Actor-Signature";

  private static final int MAX_ENCODED_LENGTH = 8192;

  private final ObjectMapper objectMapper;
  private final String secret;

  public BotActorContextCodec(
      ObjectMapper objectMapper,
      @Value("${platform.bot.actor-context-secret:}") String secret
  ) {
    this.objectMapper = objectMapper;
    this.secret = secret == null ? "" : secret.trim();
  }

  public BotActorContext decodeAndVerify(String encodedContext, String signature) {
    if (!StringUtils.hasText(encodedContext) || !StringUtils.hasText(signature)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bot actor context is required");
    }
    if (secret.length() < 32) {
      log.error("Bot actor context secret is missing or shorter than 32 characters");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Bot actor context is not configured");
    }
    if (encodedContext.length() > MAX_ENCODED_LENGTH || signature.length() > 128) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bot actor context is too large");
    }
    verifySignature(encodedContext, signature);
    try {
      byte[] jsonBytes = Base64.getUrlDecoder().decode(encodedContext);
      JsonNode json = objectMapper.readTree(jsonBytes);
      if (json == null || !json.isObject()) {
        throw invalid("Bot actor context must be an object");
      }
      return new BotActorContext(
          parseTenantId(requiredText(json, "tenant_id")),
          requiredText(json, "bot_id"),
          requiredText(json, "wecom_user_id"),
          requiredText(json, "chat_id"),
          requiredText(json, "chat_type"),
          requiredText(json, "channel_session_id"),
          requiredText(json, "weknora_session_id"),
          requiredText(json, "request_id")
      );
    } catch (IllegalArgumentException ex) {
      throw invalid("Bot actor context is not valid base64url JSON");
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw invalid("Bot actor context is not valid JSON");
    }
  }

  private void verifySignature(String payload, String suppliedSignature) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
      if (!MessageDigest.isEqual(
          expected.getBytes(StandardCharsets.US_ASCII),
          suppliedSignature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
        log.warn("Bot actor context signature verification failed");
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid bot actor context signature");
      }
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Bot actor context signature verification failed", ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Bot actor context verification failed");
    }
  }

  private String requiredText(JsonNode json, String field) {
    JsonNode value = json.get(field);
    if (value == null || !value.isTextual() || value.textValue().isBlank() || value.textValue().length() > 512) {
      throw invalid("Bot actor context field is invalid: " + field);
    }
    return value.textValue().trim();
  }

  private Long parseTenantId(String value) {
    try {
      long tenantId = Long.parseLong(value);
      if (tenantId <= 0) {
        throw new NumberFormatException();
      }
      return tenantId;
    } catch (NumberFormatException ex) {
      throw invalid("Bot actor context tenant_id is invalid");
    }
  }

  private BusinessException invalid(String message) {
    return new BusinessException(ErrorCode.UNAUTHORIZED, message);
  }
}
