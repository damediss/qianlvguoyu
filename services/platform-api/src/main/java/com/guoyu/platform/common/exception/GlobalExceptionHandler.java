package com.guoyu.platform.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.guoyu.platform.common.api.ApiResponse;
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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    return ResponseEntity.status(resolveStatus(ex.getCode()))
        .body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
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
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.fail(ErrorCode.FORBIDDEN, "Forbidden"));
  }

  /** 客户端主动断开（如下载未完成刷新页面），打 WARN 不打 ERROR */
  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
    log.warn("ClientAborted: client disconnected before response completed: {}", ex.getMessage());
  }

  private String formatFieldError(FieldError error) {
    return error.getField() + " " + error.getDefaultMessage();
  }

  private HttpStatus resolveStatus(String code) {
    return switch (code) {
      case ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case ErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN;
      case ErrorCode.NOT_FOUND -> HttpStatus.NOT_FOUND;
      case ErrorCode.CONFLICT -> HttpStatus.CONFLICT;
      case ErrorCode.UNPROCESSABLE_ENTITY -> HttpStatus.UNPROCESSABLE_ENTITY;
      case ErrorCode.BAD_GATEWAY -> HttpStatus.BAD_GATEWAY;
      default -> HttpStatus.BAD_REQUEST;
    };
  }
}
