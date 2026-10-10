package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum CollectionMatchMethodEnum {
  QR_CODE("QR_CODE", "二维码"),
  ID_CARD_LAST4("ID_CARD_LAST4", "身份证后四位"),
  NAME("NAME", "姓名"),
  ADMISSION_NO("ADMISSION_NO", "准考证号"),
  MANUAL("MANUAL", "手动");

  private final String code;
  private final String label;

  CollectionMatchMethodEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static CollectionMatchMethodEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
