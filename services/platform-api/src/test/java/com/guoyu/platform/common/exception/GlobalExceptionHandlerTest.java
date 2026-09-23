package com.guoyu.platform.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsInternalBusinessErrorToHttp500() {
    var response = handler.handleBusinessException(
        new BusinessException(ErrorCode.INTERNAL_ERROR, "配置不可用"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void mapsAnswerSyncBusinessErrorToHttp409WithData() {
    var response = handler.handleBusinessException(
        new BusinessException("ANSWERS_NOT_SYNCED", "部分答案尚未完成服务器同步",
            java.util.Map.of("questions", java.util.List.of())));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("ANSWERS_NOT_SYNCED");
    assertThat(response.getBody().data()).isNotNull();
  }

  @Test
  void mapsUnansweredConfirmToHttp409() {
    var response = handler.handleBusinessException(
        new BusinessException("UNANSWERED_CONFIRM_REQUIRED", "当前还有未作答题目，请确认后再交卷",
            java.util.Map.of("unansweredCount", 2)));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().data()).isEqualTo(java.util.Map.of("unansweredCount", 2));
  }

  @Test
  void mapsAnswerSyncRetryableToHttp503WithRetryHint() {
    var response = handler.handleBusinessException(
        new BusinessException("ANSWER_SYNC_RETRYABLE", "答案同步繁忙，请稍后重试",
            java.util.Map.of("retryAfterMs", 200)));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("ANSWER_SYNC_RETRYABLE");
    assertThat(response.getBody().data()).isEqualTo(java.util.Map.of("retryAfterMs", 200));
  }

  @Test
  void mapsDeadlockToHttp503InsteadOfGeneric500() {
    var response = handler.handleException(
        new org.springframework.dao.DeadlockLoserDataAccessException(
            "Deadlock found when trying to get lock",
            new java.sql.SQLException("Deadlock found when trying to get lock", "40001", 1213)));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().data()).isEqualTo(java.util.Map.of("retryAfterMs", 200));
  }

  @Test
  void treatsConnectionResetAsClientAbortWithoutBody() {
    // 生产真实案例：下载 application/zip 途中客户端断连，裸 SocketException 冒到通用处理器，
    // 回 JSON 会因 preset Content-Type 再炸 No converter——应识别为断连、返回 null（不写 body）
    var response = handler.handleException(new java.net.SocketException("Connection reset"));

    assertThat(response).isNull();
  }

  @Test
  void treatsWrappedBrokenPipeAsClientAbortWithoutBody() {
    var response = handler.handleException(
        new IllegalStateException("stream failed",
            new java.net.SocketException("Broken pipe")));

    assertThat(response).isNull();
  }

  @Test
  void nonAbortSocketExceptionStillMapsToHttp500() {
    // 非断连语义的 SocketException（如权限问题）不应被误吞
    var response = handler.handleException(new java.net.SocketException("Permission denied"));

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void unrelatedExceptionStillMapsToHttp500() {
    var response = handler.handleException(new IllegalStateException("boom"));

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
