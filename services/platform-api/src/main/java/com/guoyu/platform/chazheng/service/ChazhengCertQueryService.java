package com.guoyu.platform.chazheng.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.chazheng.support.FixedRateLimiter;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 按姓名+身份证查询证书（复用 sys_dict 配置，不写 chazheng 表）。
 */
@Service
@Slf4j
public class ChazhengCertQueryService {

  private final CertCheckConfigService certCheckConfigService;
  private final RestTemplate chazhengRestTemplate;
  private final ObjectMapper objectMapper;
  private final FixedRateLimiter rateLimiter;

  @Value("${chazheng.log-dir:./logs/chazheng}")
  private String chazhengLogDir;

  public ChazhengCertQueryService(
      CertCheckConfigService certCheckConfigService,
      RestTemplate chazhengRestTemplate,
      ObjectMapper objectMapper,
      @Value("${chazheng.batch.rate-limit-per-second:10.0}") double rateLimitPerSecond) {
    this.certCheckConfigService = certCheckConfigService;
    this.chazhengRestTemplate = chazhengRestTemplate;
    this.objectMapper = objectMapper;
    this.rateLimiter = FixedRateLimiter.create(rateLimitPerSecond);
  }

  public CertQueryOutcome queryByNameIdCard(String name, String idCard, Long tenantId) {
    String apiUrl = certCheckConfigService.findApiUrl(tenantId);
    if (!StringUtils.hasText(apiUrl)) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查证接口未配置(sys_dict: chazheng_url)");
    }
    List<Map<String, Object>> templates = certCheckConfigService.findRequestTemplates(tenantId);
    if (templates.isEmpty()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查证请求模板未配置(sys_dict: chazheng_datas)");
    }

    rateLimiter.acquire();

    CertCheckResult result = new CertCheckResult();
    for (int templateIdx = 0; templateIdx < templates.size(); templateIdx++) {
      Map<String, Object> template = templates.get(templateIdx);
      if (template == null) {
        continue;
      }
      Map<String, Object> reqData = new LinkedHashMap<>(template);
      reqData.put("idcode", idCard);
      reqData.put("username", name);
      processOneTemplate(apiUrl, reqData, name, idCard, templateIdx, result);
    }

    // 所有模板都没拿到正常业务响应（HTTP 错误 / 网络异常 / JSON 解析失败 / 业务层错误）
    // 说明上游查证接口故障，不能当作"成功无证"处理，必须抛异常让批次重试机制感知并记录错误
    if (!result.anyNormal) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR,
          "广西技能人才评价工作网接口报错");
    }

    return new CertQueryOutcome(result.anyNormal, result.anyCertFound, List.copyOf(result.certs));
  }

  private void processOneTemplate(
      String apiUrl,
      Map<String, Object> reqData,
      String name,
      String idCard,
      int templateIdx,
      CertCheckResult result) {
    Map<String, Object> response;
    try {
      response = chazhengRestTemplate.postForObject(
          apiUrl, new HttpEntity<>(reqData, jsonHeaders()), Map.class);
    } catch (HttpMessageConversionException e) {
      log.warn("[certQuery] JSON 解析失败 name={}, templateIdx={}, err={}",
          name, templateIdx, e.getMessage());
      logRequestResponse(name, idCard, reqData, "no_json_response", e.getMessage());
      return;
    } catch (HttpStatusCodeException e) {
      log.warn("[certQuery] HTTP 错误 name={}, templateIdx={}, status={}, body={}",
          name, templateIdx, e.getStatusCode(), abbreviate(e.getResponseBodyAsString(), 300));
      logRequestResponse(name, idCard, reqData, "http_error_" + e.getStatusCode().value(),
          abbreviate(e.getResponseBodyAsString(), 500));
      return;
    } catch (ResourceAccessException e) {
      log.warn("[certQuery] 网络异常 name={}, templateIdx={}, err={}",
          name, templateIdx, e.getMessage());
      logRequestResponse(name, idCard, reqData, "network_error", e.getMessage());
      return;
    }

    if (response == null) {
      logRequestResponse(name, idCard, reqData, "no_json_response", "null response");
      return;
    }

    if (!isBizResponseOk(response)) {
      logRequestResponse(name, idCard, reqData, "api_error", response.get("errorCode"));
      return;
    }

    result.anyNormal = true;

    Object resultObj = response.get("result");
    if (!(resultObj instanceof Map)) {
      logRequestResponse(name, idCard, reqData, "no_cert", "missing 'result' key or not a Map");
      return;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> resultMap = (Map<String, Object>) resultObj;

    Object dataObj = resultMap.get("data");
    if (!(dataObj instanceof List) || ((List<?>) dataObj).isEmpty()) {
      logRequestResponse(name, idCard, reqData, "no_cert", "data is empty");
      return;
    }

    List<?> dataList = (List<?>) dataObj;
    for (Object certObj : dataList) {
      if (!(certObj instanceof Map)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> cert = (Map<String, Object>) certObj;

      String certNo = getStringOrEmpty(cert, "serialid");
      if (certNo.isEmpty()) {
        continue;
      }

      String certName = buildCertName(getMapOrEmpty(cert, "metadata"));
      String certDate = cert.containsKey("bzz002") ? String.valueOf(cert.get("bzz002")) : "";

      result.certs.add(new CollectedCert(certNo, certName, certDate));
      result.anyCertFound = true;
    }

    logRequestResponse(name, idCard, reqData, "success", "collected " + dataList.size() + " certs");
  }

  private boolean isBizResponseOk(Map<String, Object> result) {
    Object errorCode = result.get("errorCode");
    if (errorCode != null && !errorCode.toString().isEmpty()) {
      return false;
    }
    Object statusCode = result.get("statusCode");
    if (statusCode != null && !"200".equals(statusCode.toString())) {
      return false;
    }
    Object status = result.get("status");
    if (status != null) {
      String s = status.toString();
      if (!"200".equals(s) && !"0".equals(s)) {
        return false;
      }
    }
    return true;
  }

  static String buildCertName(Map<String, Object> metadata) {
    String zymc = getStringOrEmpty(metadata, "zymc");
    String gzmcRaw = getStringOrEmpty(metadata, "gzmc");
    String zyjndj = getStringOrEmpty(metadata, "zyjndj");

    String gzmc = gzmcRaw.trim();
    if (!gzmc.isEmpty() && gzmc.matches("^[–-]+$")) {
      gzmc = "";
    }

    String base;
    if (zymc.isEmpty() && gzmc.isEmpty()) {
      base = "";
    } else if (gzmc.isEmpty()) {
      base = zymc;
    } else if (zymc.isEmpty()) {
      base = gzmc;
    } else {
      base = zymc + "_" + gzmc;
    }

    if (!base.isEmpty() && !zyjndj.isEmpty()) {
      return base + "(" + zyjndj + ")";
    } else if (!base.isEmpty()) {
      return base;
    } else if (!zyjndj.isEmpty()) {
      return zyjndj;
    }
    return "";
  }

  private static HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private static String getStringOrEmpty(Map<String, Object> map, String key) {
    Object val = map.get(key);
    return val == null ? "" : val.toString().trim();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getMapOrEmpty(Map<String, Object> map, String key) {
    Object val = map.get(key);
    return (val instanceof Map) ? (Map<String, Object>) val : new LinkedHashMap<>();
  }

  private static String abbreviate(String s, int maxLen) {
    if (s == null) {
      return "null";
    }
    return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...(truncated)";
  }

  private void logRequestResponse(
      String name,
      String idCard,
      Map<String, Object> reqData,
      String parseStatus,
      Object detail) {
    String safeUserName = name != null ? name.replaceAll("[\\\\/:*?\"<>|]", "_") : "unknown";
    Path logDir = Paths.get(chazhengLogDir);
    try {
      Files.createDirectories(logDir);
    } catch (IOException e) {
      log.warn("无法创建日志目录: {}", logDir);
      return;
    }
    Path logFile = logDir.resolve("certs_" + safeUserName + "_" + idCard + ".log");

    // 用 LinkedHashMap 而非 Map.of：Map.of 不允许 null 值，
    // 当 detail 为 null（如响应缺 errorCode 字段，仅 statusCode/status 异常）时会抛 NPE
    Map<String, Object> logPayload = new LinkedHashMap<>();
    logPayload.put("template", reqData);
    logPayload.put("detail", detail);
    String entry;
    try {
      entry = String.format("--- %s %s ---%n%s%n",
          parseStatus, Instant.now(),
          objectMapper.writeValueAsString(logPayload));
    } catch (JsonProcessingException e) {
      entry = String.format("--- %s %s ---%ntemplate=%s, detail=%s%n",
          parseStatus, Instant.now(), reqData, detail);
    }

    try {
      Files.writeString(logFile, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      log.warn("写日志失败: {}", logFile, e);
    }
  }
}
