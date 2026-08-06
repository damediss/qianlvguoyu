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
  void keepsBadRequestBusinessErrorAtHttp400() {
    var response = handler.handleBusinessException(
        new BusinessException(ErrorCode.BAD_REQUEST, "请求参数错误"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
