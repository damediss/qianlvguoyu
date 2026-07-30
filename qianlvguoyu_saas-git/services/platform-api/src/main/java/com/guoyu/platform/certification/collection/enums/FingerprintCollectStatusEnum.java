package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum FingerprintCollectStatusEnum {
  SUCCESS("SUCCESS", "成功"),
  FAILED("FAILED", "失败"),
  EXCEPTION("EXCEPTION", "异常");

  private final String code;
  private final String label;

  FingerprintCollectStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static FingerprintCollectStatusEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
