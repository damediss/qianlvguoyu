package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum CollectionExceptionStatusEnum {
  NONE("NONE", "无异常"),
  PENDING_RETRY("PENDING_RETRY", "待补采"),
  INFO_ERROR("INFO_ERROR", "信息异常"),
  RELEASED("RELEASED", "已放行"),
  RESOLVED("RESOLVED", "已处理");

  private final String code;
  private final String label;

  CollectionExceptionStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static CollectionExceptionStatusEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
