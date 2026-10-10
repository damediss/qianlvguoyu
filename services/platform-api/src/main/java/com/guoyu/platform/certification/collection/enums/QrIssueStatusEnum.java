package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum QrIssueStatusEnum {
  NOT_ISSUED("NOT_ISSUED", "未发放"),
  ISSUED("ISSUED", "已发放");

  private final String code;
  private final String label;

  QrIssueStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static QrIssueStatusEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
