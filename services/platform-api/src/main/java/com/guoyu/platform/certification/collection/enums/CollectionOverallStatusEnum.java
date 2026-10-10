package com.guoyu.platform.certification.collection.enums;

import java.util.Arrays;

public enum CollectionOverallStatusEnum {
  NOT_ISSUED("NOT_ISSUED", "未发放"),
  ISSUED("ISSUED", "已发放"),
  NOT_ARRIVED("NOT_ARRIVED", "未到场"),
  ARRIVED("ARRIVED", "已到场"),
  QUEUED("QUEUED", "已入队"),
  COLLECTING("COLLECTING", "采集中"),
  PENDING_RETRY("PENDING_RETRY", "待补采"),
  INFO_ERROR("INFO_ERROR", "异常"),
  EXCEPTION_RELEASED("EXCEPTION_RELEASED", "已放行"),
  COMPLETED("COMPLETED", "已完成"),
  CANCELLED("CANCELLED", "已取消");

  private final String code;
  private final String label;

  CollectionOverallStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public static CollectionOverallStatusEnum fromCode(String code) {
    return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst().orElse(null);
  }

  public static boolean isValid(String code) {
    return fromCode(code) != null;
  }

  public static boolean isExemptReleased(String overallStatus, String exceptionStatus) {
    return EXCEPTION_RELEASED.getCode().equals(overallStatus) || "RELEASED".equals(exceptionStatus);
  }

  public static boolean isAbnormal(String overallStatus) {
    return INFO_ERROR.getCode().equals(overallStatus);
  }

  public static boolean canExportCollectionForm(String overallStatus) {
    return canExportCollectionForm(overallStatus, null);
  }

  public static boolean canExportCollectionForm(String overallStatus, String exceptionStatus) {
    CollectionOverallStatusEnum status = fromCode(overallStatus);
    return status == COMPLETED || status == INFO_ERROR || isExemptReleased(overallStatus, exceptionStatus);
  }
}
