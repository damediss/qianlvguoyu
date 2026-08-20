package com.guoyu.platform.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.ai.infrastructure.FaqLlmClientException;
import com.guoyu.platform.ai.infrastructure.FaqLlmException;
import com.guoyu.platform.ai.infrastructure.FaqLlmRateLimitException;
import com.guoyu.platform.ai.infrastructure.FaqLlmTimeoutException;
import jakarta.validation.ConstraintViolationException;
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
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    return ResponseEntity.status(resolveStatus(ex.getCode()))
        .body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
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
  public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
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
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
