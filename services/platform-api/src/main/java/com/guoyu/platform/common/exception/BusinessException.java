package com.guoyu.platform.common.exception;

public class BusinessException extends RuntimeException {

  private final String code;
  private final Object data;

  public BusinessException(String code, String message) {
    this(code, message, null);
  }

  public BusinessException(String code, String message, Object data) {
    super(message);
    this.code = code;
    this.data = data;
  }

  public String getCode() {
    return code;
  }

  public Object getData() {
    return data;
  }
}
