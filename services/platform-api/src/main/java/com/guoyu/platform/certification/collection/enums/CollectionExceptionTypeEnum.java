package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum CollectionExceptionTypeEnum {
  DEVICE_ERROR("DEVICE_ERROR", "设备异常"),
  FINGERPRINT_UNAVAILABLE("FINGERPRINT_UNAVAILABLE", "无法采集指纹"),
  SIGNATURE_UNAVAILABLE("SIGNATURE_UNAVAILABLE", "无法完成签名"),
  INFO_ERROR("INFO_ERROR", "信息异常"),
  OTHER("OTHER", "其他");

  private final String code;
  private final String label;

  CollectionExceptionTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static CollectionExceptionTypeEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
