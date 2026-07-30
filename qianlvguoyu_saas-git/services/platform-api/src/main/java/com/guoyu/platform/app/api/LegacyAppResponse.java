package com.guoyu.platform.app.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 对齐 PHP showjson 响应格式（code 为数字，字段名为 msg） */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LegacyAppResponse(
    @JsonProperty("code") int code,
    @JsonProperty("msg") String msg,
    @JsonProperty("data") Object data
) {

  public static LegacyAppResponse ok(String msg, Object data) {
    return new LegacyAppResponse(0, msg, data);
  }

  public static LegacyAppResponse fail(int code, String msg) {
    return new LegacyAppResponse(code, msg, null);
  }

  public static LegacyAppResponse fail(int code, String msg, Object data) {
    return new LegacyAppResponse(code, msg, data);
  }
}
