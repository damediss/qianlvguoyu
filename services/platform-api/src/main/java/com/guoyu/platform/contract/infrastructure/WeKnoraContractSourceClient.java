package com.guoyu.platform.contract.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.app.coach.config.WeKnoraProperties;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Read-only, service-authenticated client for trusted WeKnora knowledge metadata and originals. */
@Component
@Slf4j
public class WeKnoraContractSourceClient {

  private static final int MAX_SOURCE_BYTES = 20 * 1024 * 1024;

  private final WeKnoraProperties properties;
  private final RestClient restClient;

  public WeKnoraContractSourceClient(WeKnoraProperties properties) {
    this.properties = properties;
    this.restClient = RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeader("X-API-Key", properties.apiKey() == null ? "" : properties.apiKey())
        .build();
  }

  public KnowledgeSourceMetadata getMetadata(String knowledgeId) {
    ensureConfigured();
    try {
      JsonNode response = restClient.get()
          .uri("/knowledge/{id}", knowledgeId)
          .retrieve()
          .body(JsonNode.class);
      if (response == null || !response.path("success").asBoolean(false)) {
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "WeKnora knowledge metadata response is invalid");
      }
      JsonNode data = response.path("data");
      return new KnowledgeSourceMetadata(
          requiredText(data, "id"),
          requiredText(data, "knowledge_base_id"),
          data.path("file_name").asText(""),
          data.path("file_type").asText("").toLowerCase(Locale.ROOT),
          data.path("file_size").asLong(0),
          data.path("file_hash").asText("").toLowerCase(Locale.ROOT),
          data.path("parse_status").asText(""),
          data.path("enable_status").asText("")
      );
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("WeKnora contract source metadata failed knowledgeId={}", knowledgeId, ex);
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "Unable to read WeKnora knowledge metadata");
    }
  }

  public byte[] downloadOriginal(String knowledgeId) {
    ensureConfigured();
    try {
      byte[] bytes = restClient.get()
          .uri("/knowledge/{id}/download", knowledgeId)
          .retrieve()
          .body(byte[].class);
      if (bytes == null || bytes.length == 0) {
        throw new BusinessException(ErrorCode.BAD_GATEWAY, "WeKnora source document is empty");
      }
      if (bytes.length > MAX_SOURCE_BYTES) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Contract source exceeds 20 MiB POC limit");
      }
      return bytes;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("WeKnora contract source download failed knowledgeId={}", knowledgeId, ex);
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "Unable to download WeKnora source document");
    }
  }

  private void ensureConfigured() {
    if (!properties.enabled() || !StringUtils.hasText(properties.baseUrl())
        || !StringUtils.hasText(properties.apiKey())) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
          "WeKnora source document access is not configured");
    }
  }

  private String requiredText(JsonNode node, String field) {
    String value = node.path(field).asText("").trim();
    if (!StringUtils.hasText(value)) {
      throw new BusinessException(ErrorCode.BAD_GATEWAY,
          "WeKnora knowledge metadata is missing " + field);
    }
    return value;
  }

  public record KnowledgeSourceMetadata(
      String knowledgeId,
      String knowledgeBaseId,
      String filename,
      String fileType,
      long fileSize,
      String fileHash,
      String parseStatus,
      String enableStatus
  ) {}
}
