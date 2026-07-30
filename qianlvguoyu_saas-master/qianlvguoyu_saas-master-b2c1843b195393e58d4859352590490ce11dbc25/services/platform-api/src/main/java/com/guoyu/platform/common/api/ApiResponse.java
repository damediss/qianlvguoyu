package com.guoyu.platform.common.api;

import com.guoyu.platform.common.web.TraceIdHolder;

public record ApiResponse<T>(String code, String message, T data, String traceId) {

  public static final String SUCCESS_CODE = "0";

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(SUCCESS_CODE, "OK", data, TraceIdHolder.getTraceId());
  }

  public static ApiResponse<Void> ok() {
    return ok(null);
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(code, message, null, TraceIdHolder.getTraceId());
  }
}
