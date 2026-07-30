package com.guoyu.platform.common.exception;

public final class ErrorCode {

  public static final String BAD_REQUEST = "400";
  public static final String UNAUTHORIZED = "401";
  public static final String FORBIDDEN = "403";
  public static final String NOT_FOUND = "404";
  public static final String CONFLICT = "409";
  public static final String INTERNAL_ERROR = "500";

  private ErrorCode() {
  }
}
