package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum CollectionReleaseScopeEnum {
  FINGERPRINT("FINGERPRINT", "指纹"),
  SIGNATURE("SIGNATURE", "签名"),
  ALL("ALL", "全部"),
  INFO("INFO", "信息"),
  OTHER("OTHER", "其他");

  private final String code;
  private final String label;

  CollectionReleaseScopeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static CollectionReleaseScopeEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
