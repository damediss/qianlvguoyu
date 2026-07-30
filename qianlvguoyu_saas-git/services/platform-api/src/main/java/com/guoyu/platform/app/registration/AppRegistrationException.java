package com.guoyu.platform.app.registration;

/** 小程序自主报名业务异常（对齐 PHP showjson code/msg） */
public class AppRegistrationException extends RuntimeException {

  private final int code;
  private final Object data;

  public AppRegistrationException(int code, String message) {
    this(code, message, null);
  }

  public AppRegistrationException(int code, String message, Object data) {
    super(message);
    this.code = code;
    this.data = data;
  }

  public int getCode() {
    return code;
  }

  public Object getData() {
    return data;
  }
}
