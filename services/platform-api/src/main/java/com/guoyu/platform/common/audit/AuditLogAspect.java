package com.guoyu.platform.common.audit;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.web.TraceIdHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.AuditLogEntity;
import com.guoyu.platform.system.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

  private static final int MAX_DETAIL_JSON_LENGTH = 60000;
  private static final int MAX_STRING_LENGTH = 512;

  private final AuditLogMapper auditLogMapper;
  private final ObjectMapper objectMapper;

  public AuditLogAspect(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
    this.auditLogMapper = auditLogMapper;
    this.objectMapper = objectMapper;
  }

  @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
  public void writeAuditLog(JoinPoint joinPoint, AuditLog auditLog, Object result) {
    PlatformUserPrincipal principal = currentPrincipal();
    if (principal == null) {
      return;
    }

    HttpServletRequest request = currentRequest();
    AuditLogEntity entity = new AuditLogEntity();
    entity.setTenantId(principal.getTenantId());
    entity.setUserId(principal.getUserId());
    entity.setModule(auditLog.module());
    entity.setAction(auditLog.action());
    entity.setTargetType(auditLog.targetType());
    entity.setTargetId(resolveTargetId(joinPoint));
    entity.setRequestId(TraceIdHolder.getTraceId());
    entity.setIp(request == null ? null : resolveClientIp(request));
    entity.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
    entity.setDetailJson(toDetailJson(joinPoint, result, auditLog));
    entity.setCreatedAt(LocalDateTime.now());
    entity.setDeleted(0);
    try {
      auditLogMapper.insert(entity);
    } catch (RuntimeException ex) {
      log.warn("Audit log insert failed for {}:{} - {}", auditLog.module(), auditLog.action(), ex.getMessage());
    }
  }

  private PlatformUserPrincipal currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof PlatformUserPrincipal principal)) {
      return null;
    }
    return principal;
  }

  private HttpServletRequest currentRequest() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
      return null;
    }
    return attributes.getRequest();
  }

  private Long resolveTargetId(JoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    if (args.length > 0 && args[0] instanceof Long id) {
      return id;
    }
    return null;
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String toDetailJson(JoinPoint joinPoint, Object result, AuditLog auditLog) {
    Map<String, Object> detail = new LinkedHashMap<>();
    detail.put("method", joinPoint.getSignature().toShortString());
    if (auditLog.includeResult()) {
      detail.put("result", sanitizeArg(result));
    } else {
      detail.put("args", sanitizeArgs(joinPoint.getArgs()));
    }
    try {
      String json = objectMapper.writeValueAsString(detail);
      if (json.length() <= MAX_DETAIL_JSON_LENGTH) {
        return json;
      }
      return json.substring(0, MAX_DETAIL_JSON_LENGTH) + "...";
    } catch (JsonProcessingException ex) {
      return "{\"error\":\"audit detail serialization failed\"}";
    }
  }

  private List<Object> sanitizeArgs(Object[] args) {
    if (args == null || args.length == 0) {
      return List.of();
    }
    List<Object> sanitized = new ArrayList<>(args.length);
    for (Object arg : args) {
      sanitized.add(sanitizeArg(arg));
    }
    return sanitized;
  }

  private Object sanitizeArg(Object arg) {
    if (arg == null) {
      return null;
    }
    if (arg instanceof byte[] bytes) {
      return Map.of("type", "byte[]", "length", bytes.length);
    }
    if (arg instanceof MultipartFile file) {
      return Map.of(
          "type", "MultipartFile",
          "name", file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
          "size", file.getSize(),
          "contentType", file.getContentType() != null ? file.getContentType() : "");
    }
    if (arg instanceof Part part) {
      return Map.of(
          "type", "Part",
          "name", part.getName() != null ? part.getName() : "",
          "size", part.getSize(),
          "submittedFileName", part.getSubmittedFileName() != null ? part.getSubmittedFileName() : "");
    }
    if (arg instanceof HttpServletRequest request) {
      return Map.of("type", "HttpServletRequest", "method", request.getMethod(), "uri", request.getRequestURI());
    }
    if (arg instanceof String text) {
      if (text.length() <= MAX_STRING_LENGTH) {
        return text;
      }
      return text.substring(0, MAX_STRING_LENGTH) + "...";
    }
    return arg;
  }
}
