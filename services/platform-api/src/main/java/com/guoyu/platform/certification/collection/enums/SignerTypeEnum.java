package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum SignerTypeEnum {
  CANDIDATE("CANDIDATE", "考生"),
  EVALUATOR("EVALUATOR", "考评员"),
  LEADER("LEADER", "组长");

  private final String code;
  private final String label;

  SignerTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static SignerTypeEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
