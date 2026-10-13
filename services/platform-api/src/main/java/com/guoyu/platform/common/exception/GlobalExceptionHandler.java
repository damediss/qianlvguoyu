package com.guoyu.platform.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.ai.infrastructure.FaqLlmClientException;
import com.guoyu.platform.ai.infrastructure.FaqLlmException;
import com.guoyu.platform.ai.infrastructure.FaqLlmRateLimitException;
import com.guoyu.platform.ai.infrastructure.FaqLlmTimeoutException;
import com.guoyu.platform.chazheng.exception.CertQueryInterfaceDownException;
import jakarta.validation.ConstraintViolationException;
import java.io.EOFException;
import java.net.SocketException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
    return ResponseEntity.status(resolveStatus(ex.getCode()))
        .body(ApiResponse.fail(ex.getCode(), ex.getMessage(), ex.getData()));
  }

  /**
   * LLM 代理调用异常 — 按子类差异化映射 HTTP 状态码。
   * 代理模式仅抛出 HTTP 级异常（429/4xx/5xx/超时），内容级结果（截断等）已透传不抛。
   */
  @ExceptionHandler(FaqLlmException.class)
  public ResponseEntity<ApiResponse<Void>> handleFaqLlmException(FaqLlmException ex) {
    log.warn("LLM 调用失败：{}", ex.getMessage());
    if (ex instanceof FaqLlmClientException) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.fail(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }
    if (ex instanceof FaqLlmRateLimitException) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body(ApiResponse.fail(ErrorCode.TOO_MANY_REQUESTS, ex.getMessage()));
    }
    if (ex instanceof FaqLlmTimeoutException) {
      return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
          .body(ApiResponse.fail(ErrorCode.GATEWAY_TIMEOUT, ex.getMessage()));
    }
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(ApiResponse.fail(ErrorCode.BAD_GATEWAY, ex.getMessage()));
  }

  @ExceptionHandler(NotLoginException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException ex) {
    log.warn("NotLoginException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.fail(ErrorCode.UNAUTHORIZED, "Not logged in or token expired"));
  }

  @ExceptionHandler(NotPermissionException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotPermission(NotPermissionException ex) {
    log.warn("NotPermissionException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.fail(ErrorCode.FORBIDDEN, "No permission"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(this::formatFieldError)
        .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.BAD_REQUEST, message));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
    return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.BAD_REQUEST, ex.getMessage()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    log.warn("Invalid request body: {}", ex.getMessage());
    return ResponseEntity.badRequest()
        .body(ApiResponse.fail(ErrorCode.BAD_REQUEST, "请求体格式错误，请检查时间格式和字段类型"));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
    log.warn("Resource not found: {}", ex.getResourcePath());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.fail(ErrorCode.NOT_FOUND, "Resource not found"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
    // 客户端中途断开（下载取消/切网/杀进程）：响应可能已预设 application/zip 等内容类型，
    // 写 JSON 500 会再炸 No converter——识别后打 WARN 直接返回 null（等价于不产出 body）
    if (isClientAbort(ex)) {
      log.warn("ClientAborted: client disconnected before response completed: {}", ex.toString());
      return null;
    }
    if (SqlLockContention.isRetryable(ex)) {
      log.warn("Retryable database lock contention: {}", ex.toString());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(ApiResponse.fail(
              ErrorCode.SERVICE_UNAVAILABLE,
              "服务繁忙，请稍后重试",
              Map.of("retryAfterMs", 200)));
    }
    // 高亮识别 Redis cache 反序列化失败（区别于业务异常，方便排查）
    Throwable rootCause = ex;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }
    String rootClass = rootCause.getClass().getName();
    if (rootClass.contains("SerializationException") || rootClass.contains("JsonProcessingException")) {
      log.error("[REDIS-CACHE-FAIL] rootCause={} msg={}",
          rootClass, rootCause.getMessage());
    }
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR, "Internal server error"));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AuthorizationDeniedException ex) {
    log.warn(
        "Request denied by method authorization, exceptionType={}",
        ex.getClass().getSimpleName());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.fail(ErrorCode.FORBIDDEN, "权限不足"));
  }

  /** 客户端主动断开（如下载未完成刷新页面），打 WARN 不打 ERROR */
  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
    log.warn("ClientAborted: client disconnected before response completed: {}", ex.getMessage());
  }

  /** 查证线程池队列已满，返回 503 提示稍后重试 */
  @ExceptionHandler(RejectedExecutionException.class)
  public ResponseEntity<ApiResponse<Void>> handleRejected(RejectedExecutionException ex) {
    log.warn("查证任务队列已满: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiResponse.fail("503", "查证服务繁忙，请稍后重试"));
  }

  /**
   * 证书查证第三方接口不可达（DNS/网络/对方 5xx）：透出故障站点和尝试次数，
   * 让前端能向用户说明是查证网站侧故障，而非平台内部错误。
   * 批处理路径在 ChazhengApplicationService 内被捕获走熔断，不会到达这里。
   */
  @ExceptionHandler(CertQueryInterfaceDownException.class)
  public ResponseEntity<ApiResponse<Void>> handleCertQueryInterfaceDown(
      CertQueryInterfaceDownException ex) {
    log.warn("[certQuery] 查证接口不可达: {}", ex.getMessage());
    String site = extractSite(ex.getApiUrl());
    String message = (site != null ? site + " " : "") + "网站接口不可达（网络故障或对方服务不可用）";
    if (ex.getAttempts() > 0) {
      message += "，共尝试 " + ex.getAttempts() + " 次";
    }
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiResponse.fail(ErrorCode.SERVICE_UNAVAILABLE, message));
  }

  /** 从完整接口地址提取站点根地址（scheme://host/），解析失败退回 null。 */
  private String extractSite(String apiUrl) {
    if (apiUrl == null || apiUrl.isBlank()) {
      return null;
    }
    try {
      java.net.URI uri = java.net.URI.create(apiUrl.trim());
      if (uri.getScheme() != null && uri.getHost() != null) {
        StringBuilder site = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() >= 0) {
          site.append(':').append(uri.getPort());
        }
        return site.append('/').toString();
      }
    } catch (IllegalArgumentException ignored) {
      // 配置值不是合法 URI 时按原样透出
    }
    return apiUrl.trim();
  }

  /**
   * 判定异常链是否为客户端断连（SocketException/EOFException 且消息为 reset/broken pipe，
   * 或消息缺失按断连处理）。裸 SocketException 在 Undertow 请求处理阶段抛出时
   * 不会包装成 AsyncRequestNotUsableException，需要在此兜底识别。
   */
  private boolean isClientAbort(Throwable ex) {
    for (Throwable t = ex; t != null && t.getCause() != t; t = t.getCause()) {
      if (t instanceof SocketException || t instanceof EOFException) {
        String message = t.getMessage();
        return message == null
            || message.contains("Connection reset")
            || message.contains("Broken pipe");
      }
    }
    return false;
  }

  private String formatFieldError(FieldError error) {
    return error.getField() + " " + error.getDefaultMessage();
  }

  private HttpStatus resolveStatus(String code) {
    return switch (code) {
      case ErrorCode.BAD_REQUEST -> HttpStatus.BAD_REQUEST;
      case ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case ErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN;
      case ErrorCode.NOT_FOUND -> HttpStatus.NOT_FOUND;
      case ErrorCode.CONFLICT -> HttpStatus.CONFLICT;
      case ErrorCode.UNPROCESSABLE_ENTITY -> HttpStatus.UNPROCESSABLE_ENTITY;
      case ErrorCode.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
      case ErrorCode.NOT_IMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
      case ErrorCode.BAD_GATEWAY -> HttpStatus.BAD_GATEWAY;
      case ErrorCode.TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
      case ErrorCode.SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
      case ErrorCode.GATEWAY_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
      case "ANSWERS_NOT_SYNCED", "UNANSWERED_CONFIRM_REQUIRED" -> HttpStatus.CONFLICT;
      case "ANSWER_SYNC_RETRYABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
