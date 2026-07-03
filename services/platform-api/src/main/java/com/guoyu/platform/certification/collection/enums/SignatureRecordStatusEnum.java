package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum SignatureRecordStatusEnum {
  ACTIVE("ACTIVE", "有效"),
  REPLACED("REPLACED", "已替换"),
  CANCELLED("CANCELLED", "已取消");

  private final String code;
  private final String label;

  SignatureRecordStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static SignatureRecordStatusEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
