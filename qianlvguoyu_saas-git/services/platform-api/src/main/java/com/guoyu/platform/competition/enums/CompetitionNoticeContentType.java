package com.guoyu.platform.competition.enums;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;

public enum CompetitionNoticeContentType {
  MARKDOWN("markdown"),
  PDF("pdf");

  private final String code;

  CompetitionNoticeContentType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return MARKDOWN.code;
    }
    for (CompetitionNoticeContentType type : values()) {
      if (type.code.equalsIgnoreCase(value.trim())) {
        return type.code;
      }
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "公告内容类型不合法");
  }
}
