package com.guoyu.platform.common.audit;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.web.TraceIdHolder;
import com.guoyu.platform.integration.bot.BotActorContext;
import com.guoyu.platform.integration.bot.BotActorContextHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.TenantScopedPrincipal;
import com.guoyu.platform.system.entity.AuditLogEntity;
import com.guoyu.platform.system.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;
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
    AuditActor actor = currentActor();
    if (actor == null) {
      return;
    }

    HttpServletRequest request = currentRequest();
    AuditLogOptions options = auditLogOptions(joinPoint);
    AuditLogEntity entity = new AuditLogEntity();
    entity.setTenantId(actor.tenantId());
    entity.setUserId(actor.userId());
    entity.setModule(auditLog.module());
    entity.setAction(auditLog.action());
    entity.setTargetType(auditLog.targetType());
    entity.setTargetId(options != null && options.targetIdFromActor()
        ? actor.userId() : resolveTargetId(joinPoint));
    entity.setRequestId(TraceIdHolder.getTraceId());
    entity.setIp(request == null ? null : resolveClientIp(request));
    entity.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
    entity.setDetailJson(toDetailJson(joinPoint, result, auditLog, options));
    entity.setCreatedAt(LocalDateTime.now());
    entity.setDeleted(0);
    try {
      auditLogMapper.insert(entity);
    } catch (RuntimeException ex) {
      log.warn("Audit log insert failed for {}:{} - {}", auditLog.module(), auditLog.action(), ex.getMessage());
    }
  }

  private AuditActor currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof PlatformUserPrincipal platformUser) {
      return new AuditActor(platformUser.getTenantId(), platformUser.getUserId());
    }
    if (principal instanceof TenantScopedPrincipal tenantPrincipal
        && principal instanceof UserDetails userDetails) {
      try {
        return new AuditActor(tenantPrincipal.getTenantId(), Long.valueOf(userDetails.getUsername()));
      } catch (NumberFormatException ex) {
        log.warn("Audit actor has a non-numeric user name: {}", userDetails.getUsername());
      }
    }
    return null;
  }

  private AuditLogOptions auditLogOptions(JoinPoint joinPoint) {
    if (!(joinPoint.getSignature() instanceof MethodSignature signature)) {
      return null;
    }
    Method method = signature.getMethod();
    return method.getAnnotation(AuditLogOptions.class);
  }

  private record AuditActor(Long tenantId, Long userId) {
  }

  private HttpServletRequest currentRequest() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
      return null;
    }
    return attributes.getRequest();
  }

  private Long resolveTargetId(JoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    if (args.length == 0 || args[0] == null) {
      return null;
    }
    Object first = args[0];
    if (first instanceof Long id) {
      return id;
    }
    if (first instanceof Integer id) {
      return id.longValue();
    }
    // 请求 DTO（record 的 id() 或普通 bean 的 getId()）也能落到审计目标 id
    try {
      java.lang.reflect.Method method;
      try {
        method = first.getClass().getMethod("id");
      } catch (NoSuchMethodException ex) {
        method = first.getClass().getMethod("getId");
      }
      Object value = method.invoke(first);
      if (value instanceof Long id) {
        return id;
      }
      if (value instanceof Integer id) {
        return id.longValue();
      }
      if (value instanceof String text && text.matches("\\d{1,19}")) {
        return Long.valueOf(text);
      }
    } catch (ReflectiveOperationException | RuntimeException ignored) {
      // 无 id 的入参（如 MultipartFile）保持 null
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

  private String toDetailJson(
      JoinPoint joinPoint, Object result, AuditLog auditLog, AuditLogOptions options) {
    Map<String, Object> detail = new LinkedHashMap<>();
    detail.put("method", joinPoint.getSignature().toShortString());
    BotActorContext actorContext = BotActorContextHolder.get();
    if (actorContext != null) {
      detail.put("botActorContext", actorContext);
    }
    if (auditLog.includeResult()) {
      detail.put("result", sanitizeArg(result));
    } else if (auditLog.includeArgs() && (options == null || options.includeArguments())) {
      detail.put("args", sanitizeArgs(joinPoint.getArgs()));
    } else if (options != null && !options.includeArguments()) {
      detail.put("args", "excluded");
    }
    // includeArgs=false 且 includeResult=false：只保留方法/操作者/时间/traceId 等
    // 元数据（教材 W3 自动保存等含整篇正文的入口必须如此，防止正文灌入审计日志）。
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
