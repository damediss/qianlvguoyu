package com.guoyu.platform.competition.enums;

public enum CompetitionNoticeAssetType {
  INLINE_IMAGE("inline_image"),
  PDF("pdf");

  private final String code;

  CompetitionNoticeAssetType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
