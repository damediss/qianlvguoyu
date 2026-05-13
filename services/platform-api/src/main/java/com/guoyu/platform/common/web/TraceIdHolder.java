package com.guoyu.platform.common.web;

public final class TraceIdHolder {

  private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

  private TraceIdHolder() {
  }

  public static String getTraceId() {
    return TRACE_ID.get();
  }

  public static void setTraceId(String traceId) {
    TRACE_ID.set(traceId);
  }

  public static void clear() {
    TRACE_ID.remove();
  }
}
