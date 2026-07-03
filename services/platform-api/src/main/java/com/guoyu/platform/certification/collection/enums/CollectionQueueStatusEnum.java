package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum CollectionQueueStatusEnum {
  NOT_QUEUED("NOT_QUEUED", "未入队"),
  QUEUED("QUEUED", "排队中"),
  COLLECTING("COLLECTING", "采集中"),
  COMPLETED("COMPLETED", "已完成"),
  REMOVED("REMOVED", "已移出"),
  CANCELLED("CANCELLED", "已取消");

  private final String code;
  private final String label;

  CollectionQueueStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static CollectionQueueStatusEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }
}
