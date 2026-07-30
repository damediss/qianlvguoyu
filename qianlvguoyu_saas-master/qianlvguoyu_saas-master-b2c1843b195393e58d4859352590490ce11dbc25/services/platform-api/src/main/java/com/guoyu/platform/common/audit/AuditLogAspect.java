package com.guoyu.platform.common.audit;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.web.TraceIdHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.AuditLogEntity;
import com.guoyu.platform.system.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

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
    entity.setDetailJson(toDetailJson(joinPoint));
    entity.setCreatedAt(LocalDateTime.now());
    entity.setDeleted(0);
    auditLogMapper.insert(entity);
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

  private String toDetailJson(JoinPoint joinPoint) {
    Map<String, Object> detail = new LinkedHashMap<>();
    detail.put("method", joinPoint.getSignature().toShortString());
    detail.put("args", joinPoint.getArgs());
    try {
      return objectMapper.writeValueAsString(detail);
    } catch (JsonProcessingException ex) {
      return "{\"error\":\"audit detail serialization failed\"}";
    }
  }
}
