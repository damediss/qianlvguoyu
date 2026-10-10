package com.guoyu.platform.app.sms;

public class AppSmsException extends RuntimeException {

  private final int code;

  public AppSmsException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
