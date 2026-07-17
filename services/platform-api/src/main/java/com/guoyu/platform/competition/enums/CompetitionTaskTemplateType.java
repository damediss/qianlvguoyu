package com.guoyu.platform.competition.enums;

public enum CompetitionTaskTemplateType {
  NOTICE_ACK("notice_ack"), CHECK_IN("check_in"), FILE_SUBMISSION("file_submission");

  private final String code;

  CompetitionTaskTemplateType(String code) { this.code = code; }
  public String getCode() { return code; }
}
