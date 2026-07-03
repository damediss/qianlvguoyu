package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum FingerprintStatusEnum {
  NOT_STARTED("NOT_STARTED", "未开始"),
  CAPTURING("CAPTURING", "采集中"),
  DONE("DONE", "已完成"),
  FAILED("FAILED", "失败"),
  EXCEPTION_RELEASED("EXCEPTION_RELEASED", "异常放行");

  private final String code;
  private final String label;

  FingerprintStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static FingerprintStatusEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
