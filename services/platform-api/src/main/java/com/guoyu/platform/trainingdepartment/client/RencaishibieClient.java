package com.guoyu.platform.trainingdepartment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.config.RencaishibieProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class RencaishibieClient {

  private final RencaishibieProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;

  public RencaishibieClient(RencaishibieProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
    requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
    this.restClient = RestClient.builder()
        .baseUrl(properties.baseUrl())
        .requestFactory(requestFactory)
        .build();
  }

  public void ensureConfigured() {
    if (!properties.enabled()) {
      throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "人才识别服务未启用");
    }
  }

  public JsonNode uploadFile(MultipartFile file, Long tenantId, Long planId) {
    ensureConfigured();
    try {
      byte[] bytes = file.getBytes();
      String filename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.xlsx";
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", new ByteArrayResource(bytes) {
        @Override
        public String getFilename() {
          return filename;
        }
      });
      return postMultipart("/api/analysis/upload", body, tenantId, planId);
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取上传文件失败：" + e.getMessage());
    }
  }

  public JsonNode startAnalysis(long inputFileId, Long tenantId, Long planId) {
    ensureConfigured();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("input_file_id", inputFileId);
    body.put("strict_mode", false);
    body.put("job_resolver", "rules_then_llm");
    body.put("enable_batch_llm", true);
    body.put("enable_company_filter", true);
    return postJson("/api/analysis/run", body, tenantId, planId);
  }

  public JsonNode getAnalysisStatus(long taskId, Long tenantId, Long planId) {
    ensureConfigured();
    return getJson("/api/analysis/" + taskId + "/status", tenantId, planId);
  }

  public byte[] downloadPlan(long taskId, Long tenantId, Long planId) {
    ensureConfigured();
    return getBytes("/api/analysis/" + taskId + "/download-plan", tenantId, planId);
  }

  public byte[] downloadZip(long taskId, Long tenantId, Long planId) {
    ensureConfigured();
    return getBytes("/api/analysis/" + taskId + "/download", tenantId, planId);
  }

  public JsonNode getDistribution(long taskId, Long tenantId, Long planId) {
    ensureConfigured();
    return getJson("/api/review/" + taskId + "/distribution", tenantId, planId);
  }

  public JsonNode listEmployees(long taskId, int page, int pageSize, Long tenantId, Long planId) {
    ensureConfigured();
    return getJson("/api/review/" + taskId + "/employees?page=" + page + "&page_size=" + pageSize,
        tenantId, planId);
  }

  public JsonNode finalizeReview(long taskId, Long tenantId, Long planId) {
    ensureConfigured();
    return postJson("/api/review/" + taskId + "/finalize", Map.of(), tenantId, planId);
  }

  private JsonNode postJson(String path, Object body, Long tenantId, Long planId) {
    try {
      String json = objectMapper.writeValueAsString(body);
      String response = restClient.post()
          .uri(path)
          .headers(h -> applyHeaders(h, tenantId, planId))
          .contentType(MediaType.APPLICATION_JSON)
          .body(json)
          .retrieve()
          .body(String.class);
      return objectMapper.readTree(response == null ? "{}" : response);
    } catch (RestClientResponseException e) {
      throw translateError(e);
    } catch (Exception e) {
      log.error("Rencaishibie POST {} failed", path, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "人才识别服务调用失败：" + e.getMessage());
    }
  }

  private JsonNode postMultipart(String path, MultiValueMap<String, Object> body, Long tenantId, Long planId) {
    try {
      String response = restClient.post()
          .uri(path)
          .headers(h -> applyHeaders(h, tenantId, planId))
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(body)
          .retrieve()
          .body(String.class);
      return objectMapper.readTree(response == null ? "{}" : response);
    } catch (RestClientResponseException e) {
      throw translateError(e);
    } catch (Exception e) {
      log.error("Rencaishibie multipart POST {} failed", path, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "人才识别文件上传失败：" + e.getMessage());
    }
  }

  private JsonNode getJson(String path, Long tenantId, Long planId) {
    try {
      String response = restClient.get()
          .uri(path)
          .headers(h -> applyHeaders(h, tenantId, planId))
          .retrieve()
          .body(String.class);
      return objectMapper.readTree(response == null ? "{}" : response);
    } catch (RestClientResponseException e) {
      throw translateError(e);
    } catch (Exception e) {
      log.error("Rencaishibie GET {} failed", path, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "人才识别服务查询失败：" + e.getMessage());
    }
  }

  private byte[] getBytes(String path, Long tenantId, Long planId) {
    try {
      return restClient.get()
          .uri(path)
          .headers(h -> applyHeaders(h, tenantId, planId))
          .retrieve()
          .body(byte[].class);
    } catch (RestClientResponseException e) {
      throw translateError(e);
    } catch (Exception e) {
      log.error("Rencaishibie download {} failed", path, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "人才识别文件下载失败：" + e.getMessage());
    }
  }

  private void applyHeaders(org.springframework.http.HttpHeaders headers, Long tenantId, Long planId) {
    if (tenantId != null) {
      headers.set("X-Tenant-Id", String.valueOf(tenantId));
    }
    if (planId != null) {
      headers.set("X-Plan-Id", String.valueOf(planId));
    }
    // 转发当前请求的 Authorization header（用户 token，含 Bearer 前缀）
    String authorization = getCurrentAuthorization();
    if (StringUtils.hasText(authorization)) {
      headers.set("Authorization", authorization);
    }
    if (StringUtils.hasText(properties.internalToken())) {
      headers.set("X-Internal-Token", properties.internalToken());
    }
  }

  /**
   * 从当前 HTTP 请求线程提取 Authorization header。
   * 返回原始值（含 "Bearer " 前缀），直接转发给 Python。
   * 仅在 HTTP 请求线程有效；异步线程返回 null。
   */
  private String getCurrentAuthorization() {
    var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
    if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttrs) {
      return servletAttrs.getRequest().getHeader("Authorization");
    }
    return null;
  }

  private BusinessException translateError(RestClientResponseException e) {
    String detail = e.getResponseBodyAsString();
    log.warn("Rencaishibie HTTP {}: {}", e.getStatusCode().value(), detail);
    if (StringUtils.hasText(detail)) {
      try {
        JsonNode node = objectMapper.readTree(detail);
        if (node.has("detail")) {
          return new BusinessException(ErrorCode.BAD_REQUEST, node.get("detail").asText());
        }
        if (node.has("message")) {
          return new BusinessException(ErrorCode.BAD_REQUEST, node.get("message").asText());
        }
      } catch (Exception ignored) {
        // fall through
      }
    }
    return new BusinessException(ErrorCode.INTERNAL_ERROR,
        "人才识别服务异常(HTTP " + e.getStatusCode().value() + ")");
  }
}
