package com.guoyu.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.web.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

  @AfterEach
  void tearDown() {
    TraceIdHolder.clear();
  }

  @Test
  void okUsesCurrentTraceId() {
    TraceIdHolder.setTraceId("req-test");

    ApiResponse<String> response = ApiResponse.ok("pong");

    assertThat(response.code()).isEqualTo("0");
    assertThat(response.message()).isEqualTo("OK");
    assertThat(response.data()).isEqualTo("pong");
    assertThat(response.traceId()).isEqualTo("req-test");
  }
}
